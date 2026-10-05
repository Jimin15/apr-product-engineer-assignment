package com.apr.community.common.docs;

/**
 * API 문서 전용: 요청 · 응답 스키마의 설명. DTO 에 문서 애너테이션을 두지 않으려고 기능별 *ApiDocs 에 모아 두고,
 * OpenApiConfig 가 문서를 만들 때 해당 스키마에 붙인다.
 *
 * @param schema      스키마 이름 (DTO 클래스 이름, 예: PostResponse)
 * @param field       필드 이름. null 이면 스키마 자체의 설명
 * @param description 설명
 * @param example     예시 값 (숫자 · 불리언 필드는 그 타입으로 바꿔 넣는다). 없으면 null
 */
public record SchemaDoc(String schema, String field, String description, String example) {

    public static SchemaDoc type(String schema, String description) {
        return new SchemaDoc(schema, null, description, null);
    }

    public static SchemaDoc field(String schema, String field, String description, String example) {
        return new SchemaDoc(schema, field, description, example);
    }

    public static SchemaDoc field(String schema, String field, String description) {
        return new SchemaDoc(schema, field, description, null);
    }
}
