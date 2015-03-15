package springdox.documentation.spring.web.readers.operation;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import springdox.documentation.schema.Collections;
import springdox.documentation.schema.Maps;
import springdox.documentation.schema.ModelRef;
import springdox.documentation.schema.TypeNameExtractor;
import springdox.documentation.spi.DocumentationType;
import springdox.documentation.spi.schema.contexts.ModelContext;
import springdox.documentation.spi.service.OperationBuilderPlugin;
import springdox.documentation.spi.service.contexts.OperationContext;

import static springdox.documentation.spi.schema.contexts.ModelContext.*;
import static springdox.documentation.spring.web.HandlerMethodReturnTypes.*;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OperationResponseClassReader implements OperationBuilderPlugin {
  private static Logger log = LoggerFactory.getLogger(OperationResponseClassReader.class);
  private final TypeResolver typeResolver;
  private final TypeNameExtractor nameExtractor;

  @Autowired
  public OperationResponseClassReader(TypeResolver typeResolver,
                                      TypeNameExtractor nameExtractor) {
    this.typeResolver = typeResolver;
    this.nameExtractor = nameExtractor;
  }

  @Override
  public void apply(OperationContext context) {
    HandlerMethod handlerMethod = context.getHandlerMethod();
    ResolvedType returnType = handlerReturnType(typeResolver, handlerMethod);
    returnType = context.alternateFor(returnType);
    ModelContext modelContext = returnValue(returnType, context.getDocumentationType(),
            context.getAlternateTypeProvider());
    String responseTypeName = nameExtractor.typeName(modelContext);
    log.debug("Setting spring response class to:" + responseTypeName);
    context.operationBuilder()
            .responseClass(responseTypeName)
            .responseModel(modelRef(returnType, modelContext))
    ;
  }

  private ModelRef modelRef(ResolvedType type, ModelContext modelContext) {
    if (Collections.isContainerType(type)) {
      ResolvedType collectionElementType = Collections.collectionElementType(type);
      String elementTypeName = nameExtractor.typeName(fromParent(modelContext, collectionElementType));
      return new ModelRef(Collections.containerType(type), elementTypeName);
    }
    if (Maps.isMapType(type)) {
      String elementTypeName = nameExtractor.typeName(fromParent(modelContext, Maps.mapValueType(type)));
      return new ModelRef("Map", elementTypeName, true);
    }
    String typeName = nameExtractor.typeName(fromParent(modelContext, type));
    return new ModelRef(typeName);
  }

  @Override
  public boolean supports(DocumentationType delimiter) {
    return true;
  }
}