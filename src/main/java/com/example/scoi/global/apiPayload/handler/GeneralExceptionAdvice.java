package com.example.scoi.global.apiPayload.handler;

import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import com.example.scoi.global.apiPayload.code.GeneralErrorCode;
import com.example.scoi.global.apiPayload.exception.ScoiException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.UnexpectedTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.*;

@Slf4j
@RestControllerAdvice
public class GeneralExceptionAdvice {

    // 컨트롤러 메서드에서 @Valid 어노테이션을 사용하여 DTO의 유효성 검사를 수행
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex
    ) {
        log.warn("[ MethodArgumentNotValidException ]: 검증에 실패했습니다.");
        // 검사에 실패한 필드와 그에 대한 메시지를 저장하는 Map
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        BaseErrorCode validationErrorCode = GeneralErrorCode.VALIDATION_FAILED; // BaseErrorCode로 통일
        ApiResponse<Map<String, String>> errorResponse = ApiResponse.onFailure(
                validationErrorCode,
                errors
        );
        // 에러 코드, 메시지와 함께 errors를 반환
        return ResponseEntity.status(validationErrorCode.getStatus()).body(errorResponse);
    }

    // 쿼리 파라미터 검증
    @ExceptionHandler(HandlerMethodValidationException.class)
    protected ResponseEntity<ApiResponse<Map<String, String>>> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex
    ) {
        log.warn("[ HandlerMethodValidationException ]: 쿼리 파라미터 검증에 실패했습니다.");
        Map<String, String> errors = new HashMap<>();
        ex.getParameterValidationResults().forEach(result ->
                errors.put(result.getMethodParameter().getParameterName(), result.getResolvableErrors().getFirst().getDefaultMessage()));
        BaseErrorCode validationErrorCode = GeneralErrorCode.VALIDATION_FAILED; // BaseErrorCode로 통일
        ApiResponse<Map<String, String>> errorResponse = ApiResponse.onFailure(
                validationErrorCode,
                errors
        );
        // 에러 코드, 메시지와 함께 errors를 반환
        return ResponseEntity.status(validationErrorCode.getStatus()).body(errorResponse);

    }

    // 요청 파라미터가 없을 때 발생하는 예외 처리
    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ResponseEntity<ApiResponse<String>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex
    ) {

        log.warn("[ MissingRequestParameterException ]: 필요한 파라미터가 요청에 없습니다.");
        BaseErrorCode validationErrorCode = GeneralErrorCode.VALIDATION_FAILED; // BaseErrorCode로 통일
        ApiResponse<String> errorResponse = ApiResponse.onFailure(
                validationErrorCode,
                ex.getParameterName()+" 파라미터가 없습니다."
        );
        // 에러 코드, 메시지와 함께 errors를 반환
        return ResponseEntity.status(validationErrorCode.getStatus()).body(errorResponse);
    }

    // ConstraintViolationException 핸들러
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException ex
    ) {
        // 제약 조건 위반 정보를 저장할 Map
        Map<String, String> errors = new HashMap<>();

        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            // 마지막 필드명만 추출 (예: user.name -> name)
            String fieldName = propertyPath.contains(".") ?
                    propertyPath.substring(propertyPath.lastIndexOf(".") + 1) : propertyPath;

            errors.put(fieldName, violation.getMessage());
        });

        BaseErrorCode constraintErrorCode = GeneralErrorCode.VALIDATION_FAILED;
        ApiResponse<Map<String, String>> errorResponse = ApiResponse.onFailure(
                constraintErrorCode,
                errors
        );

        log.warn("[ ConstraintViolationException ]: Constraint violations detected");

        return ResponseEntity.status(constraintErrorCode.getStatus()).body(errorResponse);
    }

    // IllegalArgumentException 핸들러 (enum 변환 실패 등)
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(
            IllegalArgumentException ex
    ) {
        log.warn("[ IllegalArgumentException ]: {}", ex.getMessage());
        
        // ExchangeType 변환 실패인 경우 InvestErrorCode 사용
        if (ex.getMessage() != null && ex.getMessage().contains("Invalid exchange type")) {
            com.example.scoi.domain.invest.exception.code.InvestErrorCode investErrorCode = 
                    com.example.scoi.domain.invest.exception.code.InvestErrorCode.INVALID_EXCHANGE_TYPE;
            ApiResponse<String> errorResponse = ApiResponse.onFailure(
                    investErrorCode,
                    null
            );
            return ResponseEntity.status(investErrorCode.getStatus()).body(errorResponse);
        }
        
        // 그 외의 경우 일반 검증 실패로 처리
        BaseErrorCode validationErrorCode = GeneralErrorCode.VALIDATION_FAILED;
        ApiResponse<String> errorResponse = ApiResponse.onFailure(
                validationErrorCode,
                ex.getMessage()
        );
        return ResponseEntity.status(validationErrorCode.getStatus()).body(errorResponse);
    }

    // Request Body 파싱 실패, Request Body 미포함
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex
    ) {

        // 만약 Request Body 파싱에 실패한 경우
        if (ex.getCause() instanceof InvalidFormatException e) {
            log.warn("[ InvalidFormatException ]: Request Body 파싱에 실패했습니다.");

            Map<String, String> errors = new HashMap<>();

            // 만약 빈칸인 경우
            if (e.getValue().toString().isEmpty()) {
                errors.put(e.getPath().getFirst().getPropertyName(), "빈칸을 변환할 수 없습니다.");
            } else {
                errors.put(e.getPath().getFirst().getPropertyName(), e.getValue().toString()+"을 변환할 수 없습니다.");
            }
            ApiResponse<Map<String, String>> errorResponse = ApiResponse.onFailure(
                    GeneralErrorCode.JSON_PARSE_FAIL,
                    errors
            );
            return ResponseEntity.status(GeneralErrorCode.JSON_PARSE_FAIL.getStatus()).body(errorResponse);
        } else if (ex.getMessage().contains("JSON parse error:")){
            log.warn("[  ]: Request Body 파싱에 실패했습니다.");

            ApiResponse<Void> errorResponse = ApiResponse.onFailure(
                    GeneralErrorCode.JSON_PARSE_FAIL,
                    null
            );
            return ResponseEntity.status(GeneralErrorCode.JSON_PARSE_FAIL.getStatus()).body(errorResponse);
        }

        // Request Body가 없는 경우
        log.warn("[ HttpMessageNotReadableException ]: Request Body가 없습니다.");
        BaseErrorCode validationErrorCode = GeneralErrorCode.NOT_FOUND_REQUEST_BODY;
        ApiResponse<Void> errorResponse = ApiResponse.onFailure(
                validationErrorCode,
                null
        );
        return ResponseEntity.status(validationErrorCode.getStatus()).body(errorResponse);
    }

    // @Valid 검증하려 하지만 들어온 타입이 다른 경우
    @ExceptionHandler(UnexpectedTypeException.class)
    public ResponseEntity<ApiResponse<String>> handleUnexpectedTypeException(
            UnexpectedTypeException ex
    ) {
        log.warn("[ UnexpectedTypeException ]: @Valid 검증이 타입 차이때문에 실패했습니다.");

        List<String> e = Arrays.stream(ex.getLocalizedMessage().split("'")).toList();
        String errorClass = e.getLast();
        BaseErrorCode validationErrorCode = GeneralErrorCode.VALIDATION_FAILED;
        ApiResponse<String> errorResponse = ApiResponse.onFailure(
                validationErrorCode,
                errorClass+"과 요청의 타입 차이 때문에 실패했습니다."
        );
        return ResponseEntity.status(validationErrorCode.getStatus()).body(errorResponse);
    }

    // 지원하지 않는 메서드
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Map<String, String[]>>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex
    ){
        log.warn("[ HttpRequestMethodNotSupportedException ]: 해당 엔드포인트는 해당 메서드를 지원하지 않습니다.");

        BaseErrorCode code = GeneralErrorCode.NOT_SUPPORT_HTTP_METHOD;

        // 지원하는 HTTP 메서드 나열
        Map<String, String[]> supportedMethods = new HashMap<>();
        supportedMethods.put("지원하는 메서드", ex.getSupportedMethods());

        ApiResponse<Map<String, String[]>> errorResponse = ApiResponse.onFailure(code, supportedMethods);
        return ResponseEntity.status(code.getStatus()).body(errorResponse);
    }

    // 없는 URI에 요청이 들어온 경우
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(
            NoResourceFoundException ex
    ){
        log.warn("[ NoResourceFoundException ]: 존재하지 않는 URI에 요청을 보냈습니다.");

        BaseErrorCode code = GeneralErrorCode.NOT_FOUND_URI;
        ApiResponse<Void> errorResponse = ApiResponse.onFailure(code, null);
        return ResponseEntity.status(code.getStatus()).body(errorResponse);
    }

    // NPE: 주로 DTO 검증부분이 약하거나 비즈니스 로직에서 발생
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<String>> handleNullPointerException(
            NullPointerException ex
    ){
        log.warn("[ NullPointerException ]: NPE가 발생했습니다. 파일명: {}, 발생 코드라인: {}",
                ex.getStackTrace()[0].getFileName(), ex.getStackTrace()[0].getLineNumber());

        // result에 어디서 발생했는지 표시
        BaseErrorCode code = GeneralErrorCode.INTERNAL_SERVER_ERROR;
        ApiResponse<String> errorResponse = ApiResponse.onFailure(code, ex.getStackTrace()[0].getFileName());
        return ResponseEntity.status(code.getStatus()).body(errorResponse);
    }

    // 쿼리파라미터 타입 불일치
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex
    ){
        log.warn("[ MethodArgumentTypeMismatchException ]: 쿼리 파라미터 타입이 맞지 않습니다.");

        BaseErrorCode code = GeneralErrorCode.PARAMETER_MISMATCH;
        Map<String, String> errorResponse = new HashMap<>();

        if (ex.getValue() != null){
            errorResponse.put(ex.getName(), ex.getValue().toString());
        } else {
            errorResponse.put(ex.getName(), "null");
        }
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code, errorResponse));
    }

    // Content-Type 불일치
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException ex
    ){
        log.warn("[ HttpMediaTypeNotSupportedException ]: 지원하지 않는 Content-Type입니다.");

        BaseErrorCode code = GeneralErrorCode.NOT_SUPPORT_CONTENT_TYPE;
        return ResponseEntity.status(code.getStatus()).body(
                ApiResponse.onFailure(code, Objects.requireNonNull(ex.getContentType()).getSubtype())
        );
    }

    // 애플리케이션에서 발생하는 커스텀 예외를 처리
    @ExceptionHandler(ScoiException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(
            ScoiException ex
    ) {
        // 예외가 발생하면 로그 기록
        log.warn("[ ScoiException ]: {}", ex.getCode().getMessage());

        // 바인딩이 존재하는 경우
        if (ex.getBind() != null) {
            return ResponseEntity.status(ex.getCode().getStatus())
                    .body(ApiResponse.onFailure(
                            ex.getCode(),
                            ex.getBind()
                    ));
        }
        // 커스텀 예외에 정의된 에러 코드와 메시지를 포함한 응답 제공
        return ResponseEntity.status(ex.getCode().getStatus())
                .body(ApiResponse.onFailure(
                                ex.getCode(),
                                null
                        )
                );
    }

    // 그 외의 정의되지 않은 모든 예외 처리
    @ExceptionHandler({Exception.class})
    public ResponseEntity<ApiResponse<String>> handleAllException(
            Exception ex
    ) {
        log.error("[WARNING] Internal Server Error : {} ", ex.getMessage());
        BaseErrorCode errorCode = GeneralErrorCode.INTERNAL_SERVER_ERROR;
        ApiResponse<String> errorResponse = ApiResponse.onFailure(
                errorCode,
                null
        );
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(errorResponse);
    }
}
