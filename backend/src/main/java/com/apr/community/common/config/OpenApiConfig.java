package com.apr.community.common.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.method.HandlerMethod;

import com.apr.community.comment.CommentApiDocs;
import com.apr.community.common.docs.ApiErrorCodes;
import com.apr.community.common.docs.SchemaDoc;
import com.apr.community.common.error.ErrorCode;
import com.apr.community.common.error.ErrorResponse;
import com.apr.community.common.user.CurrentUser;
import com.apr.community.common.user.RequesterIdResolver;
import com.apr.community.like.LikeApiDocs;
import com.apr.community.post.PostApiDocs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;

/**
 * Swagger UI (/swagger-ui.html) · OpenAPI 문서 (/v3/api-docs).
 * API 별 설명 · 오류 코드 · 스키마 설명은 기능별 *ApiDocs 에 있고, 여기는 그것을 문서에 붙이는 장치만 둔다.
 * - @CurrentUser String 파라미터는 Resolver 가 헤더 · 쿠키에서 채우므로 문서에서 숨기고, 그 API 에 x-user-id 헤더를 선택 항목으로 붙인다.
 * - 오류 응답은 @ApiErrorCodes 와 자동 규칙(요청자 → USER_ID_MISMATCH, 경로 변수 → INVALID_INPUT)으로 모은 ErrorCode 에서
 *   실제 응답과 같은 예시 JSON 을 만든다. 같은 상태(예: 404)의 여러 코드는 예시 여러 개로 보여준다.
 */
@Configuration
public class OpenApiConfig {

    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
    }

    @Bean
    public OpenAPI communityOpenApi() {
        return new OpenAPI().info(new Info().title("메디큐브톡 커뮤니티 API").version("v1"));
    }

    /** 기능별 *ApiDocs.SCHEMAS 를 요청 · 응답 스키마에 붙인다. 없는 스키마 · 필드를 적으면 문서 생성 때 바로 실패한다. */
    @Bean
    public OpenApiCustomizer schemaDocs() {
        return openApi -> {
            List<SchemaDoc> docs = new ArrayList<>(PostApiDocs.SCHEMAS);
            docs.addAll(CommentApiDocs.SCHEMAS);
            docs.addAll(LikeApiDocs.SCHEMAS);
            docs.forEach(doc -> apply(openApi.getComponents().getSchemas(), doc));
        };
    }

    @SuppressWarnings("rawtypes")
    private static void apply(Map<String, Schema> schemas, SchemaDoc doc) {
        Schema schema = schemas.get(doc.schema());
        if (schema == null) {
            throw new IllegalStateException("API 문서: 없는 스키마 " + doc.schema());
        }
        if (doc.field() == null) {
            schema.setDescription(doc.description());
            return;
        }
        Schema property = schema.getProperties() == null ? null : (Schema) schema.getProperties().get(doc.field());
        if (property == null) {
            throw new IllegalStateException("API 문서: " + doc.schema() + " 에 없는 필드 " + doc.field());
        }
        property.setDescription(doc.description());
        if (doc.example() != null) {
            property.setExample(typedExample(property, doc.example()));
        }
    }

    /** 예시는 문자열로 적고, 필드 타입에 맞춰 숫자 · 불리언으로 바꿔 넣는다 (Swagger UI 의 예시 JSON 이 실제 타입과 같도록). */
    @SuppressWarnings("rawtypes")
    private static Object typedExample(Schema property, String example) {
        String type = property.getType() != null ? property.getType()
                : property.getTypes() != null && !property.getTypes().isEmpty() ? (String) property.getTypes().iterator().next() : "string";
        return switch (type) {
            case "integer" -> Long.valueOf(example);
            case "number" -> Double.valueOf(example);
            case "boolean" -> Boolean.valueOf(example);
            default -> example;
        };
    }

    @Bean
    public OperationCustomizer requesterAndErrors() {
        return (operation, handlerMethod) -> {
            boolean usesRequester = hasParameterAnnotation(handlerMethod, CurrentUser.class);
            if (usesRequester) {
                operation.addParametersItem(new HeaderParameter()
                        .name(RequesterIdResolver.HEADER_NAME)
                        .required(false)
                        .description("요청자 id. 생략하면 쿠키 `x-user-id`, 그것도 없으면 `" + RequesterIdResolver.DEFAULT_USER
                                + "`. 헤더와 쿠키가 둘 다 있는데 다르면 400 `USER_ID_MISMATCH`")
                        .schema(new StringSchema().example("Jelin")));
            }
            addErrorResponses(operation, collectErrorCodes(handlerMethod, usesRequester));
            return operation;
        };
    }

    /** @ApiErrorCodes 에 적은 코드 + 자동 규칙 (요청자 → USER_ID_MISMATCH, 경로 변수 → INVALID_INPUT). */
    private static Set<ErrorCode> collectErrorCodes(HandlerMethod handlerMethod, boolean usesRequester) {
        Set<ErrorCode> codes = EnumSet.noneOf(ErrorCode.class);
        ApiErrorCodes declared = handlerMethod.getMethodAnnotation(ApiErrorCodes.class);   // 구현한 *ApiDocs 인터페이스의 애너테이션까지 찾는다
        if (declared != null) {
            codes.addAll(List.of(declared.value()));
        }
        if (usesRequester) {
            codes.add(ErrorCode.USER_ID_MISMATCH);
        }
        if (hasParameterAnnotation(handlerMethod, PathVariable.class)) {
            codes.add(ErrorCode.INVALID_INPUT);
        }
        return codes;
    }

    /** 상태별 오류 응답. 본문 형태는 예시 JSON 으로 보여준다 (별도 스키마를 두지 않는다). */
    private static void addErrorResponses(Operation operation, Set<ErrorCode> codes) {
        Map<Integer, List<ErrorCode>> byStatus = codes.stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.groupingBy(code -> code.status().value(), TreeMap::new, Collectors.toList()));

        byStatus.forEach((status, statusCodes) -> {
            Map<String, Example> examples = new LinkedHashMap<>();
            for (ErrorCode code : statusCodes) {
                examples.put(code.name(), new Example().summary(code.message()).value(exampleBody(code)));
            }
            String description = statusCodes.stream().map(ErrorCode::name).collect(Collectors.joining(" · "));
            operation.getResponses().addApiResponse(String.valueOf(status), new ApiResponse()
                    .description(description)
                    .content(new Content().addMediaType("application/json", new MediaType().examples(examples))));
        });
    }

    /** 실제 응답과 같은 본문. INVALID_INPUT 은 errors 목록이 있는 형태를 보여준다. */
    private static ErrorResponse exampleBody(ErrorCode code) {
        if (code == ErrorCode.INVALID_INPUT) {
            return ErrorResponse.invalidInput(List.of(new ErrorResponse.FieldError("title", "제목을 입력해 주세요.")));
        }
        return ErrorResponse.of(code);
    }

    private static boolean hasParameterAnnotation(HandlerMethod handlerMethod, Class<? extends java.lang.annotation.Annotation> type) {
        return Arrays.stream(handlerMethod.getMethodParameters()).anyMatch(p -> p.hasParameterAnnotation(type));
    }
}
