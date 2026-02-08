# 智码 (CodeHub) 后端开发规范 v1.0

## 1. 统一响应格式
所有 HTTP 接口必须返回 `Result<T>` 包装类，严禁直接返回实体对象或 `void`。
- 成功：`Result.success(data)`
- 失败：`Result.error(code, message)`

## 2. 异常处理原则
- 业务层 (Service) 禁止捕获异常而不抛出，必须让 `GlobalExceptionHandler` 统一处理。
- 禁止使用 `e.printStackTrace()`，必须使用 `log.error("异常描述", e)`。

## 3. 时间格式
- 数据库字段强制使用 `datetime` 类型。
- Java 实体类强制使用 `java.time.LocalDateTime`，严禁使用 `java.util.Date`。
- 前端交互统一格式：`yyyy-MM-dd HH:mm:ss`。

## 4. 智码特有注解
- `@CodeHubSecured`: 用于标记需要内部鉴权的接口。
- `@LogAudit`: 用于标记需要记录操作日志的方法。