# 安全扫描指南

## 项目安全扫描配置

本项目已配置多种安全扫描工具，用于检测代码和依赖项中的安全漏洞。

## 可用的安全扫描工具

### 1. SpotBugs (已集成)
- 已在 `pom.xml` 中配置
- 用于检测 Java 代码中的 bug 和安全漏洞

### 2. PMD (已集成)
- 已在 `pom.xml` 中配置
- 用于检测代码质量和安全问题

### 3. OWASP Dependency Check (建议添加)

#### 如何运行依赖安全检查
```bash
# 使用 Maven 插件运行依赖安全检查
mvn org.owasp:dependency-check-maven:10.0.0:check

# 或者使用 Docker
docker run --rm -v $(pwd):/src owasp/dependency-check --scan /src --format XML --format HTML
```

### 4. OWASP ZAP (Zed Attack Proxy)
- 主要用于 Web 应用程序安全测试
- 对于这个 Java 库项目，建议使用其他工具

## 代码安全分析 (ArrayStack.java 和 ArrayUtils.java)

### ArrayStack.java 分析
- 继承自 `ArrayList`，因此是可序列化的
- 没有发现反序列化安全问题（与项目中之前修复的 COLLECTIONS-580 不同）
- 该类已标记为 `@Deprecated`，建议使用 `java.util.ArrayDeque`
- 代码简洁，没有明显的安全漏洞

### ArrayUtils.java 分析
- 包级私有工具类
- 不可序列化（没有实现 `Serializable`）
- 提供基本的数组操作功能
- 没有发现安全问题

## 运行安全扫描

```bash
# 运行完整的项目构建和现有安全检查
mvn clean verify apache-rat:check pmd:check checkstyle:check japicmp:cmp javadoc:javadoc

# 运行 SpotBugs
mvn spotbugs:check

# 运行 PMD
mvn pmd:check
```

## 安全最佳实践

1. **反序列化安全**: 项目已在 COLLECTIONS-580 中修复了不安全的反序列化问题
2. **依赖管理**: 定期更新依赖项，使用 OWASP Dependency Check 检测已知漏洞
3. **代码审查**: 使用 SpotBugs、PMD 和 Checkstyle 进行自动代码审查
4. **文档记录**: 保持 security.xml 文档更新，记录已知的安全修复和建议
