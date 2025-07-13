# Contributing to Merchant Center

Thank you for your interest in contributing to Merchant Center! This document provides guidelines and information for contributors.

## 🚀 Getting Started

### Prerequisites
- Java 21 or higher
- Gradle 8.0 or higher
- Kotlin 2.0.21 or higher
- Git

### Development Setup
1. Fork the repository
2. Clone your fork: `git clone https://github.com/la-niina/merchant-center`
3. Create a feature branch: `git checkout -b feature/your-feature-name`
4. Make your changes
5. Test your changes thoroughly
6. Commit your changes: `git commit -m "feat: add your feature description"`
7. Push to your fork: `git push origin feature/your-feature-name`
8. Create a Pull Request

## 📋 Development Guidelines

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions small and focused
- Use proper indentation (4 spaces)

### Database Changes
- Always include migration scripts
- Test migrations with existing data
- Ensure backward compatibility
- Update schema documentation

### UI/UX Guidelines
- Follow Material Design 3 principles
- Ensure accessibility compliance
- Test on different screen sizes
- Provide clear error messages
- Add loading states for async operations

### Testing Requirements
- Write unit tests for new functionality
- Test database migrations thoroughly
- Perform manual testing on critical paths
- Ensure no regression in existing features

## 🏗️ Architecture Guidelines

### Package Structure
```
src/main/kotlin/
├── core/           # Core utilities, validation, database
├── domain/         # Domain models and business logic
├── presentation/   # UI components and screens
├── navigation/     # Navigation logic
└── viewmodel/      # ViewModels and state management
```

### Data Flow
1. **UI Layer**: Presentation components and user interactions
2. **ViewModel Layer**: Business logic and state management
3. **Domain Layer**: Data models and validation
4. **Data Layer**: Database operations and external APIs

### Database Guidelines
- Use SQLDelight for type-safe database operations
- Include proper constraints and indexes
- Implement audit trails for data changes
- Provide migration scripts for schema changes

## 🧪 Testing Guidelines

### Unit Tests
- Test all validation logic
- Test database operations
- Test ViewModel business logic
- Mock external dependencies

### Integration Tests
- Test database migrations
- Test complete user workflows
- Test data integrity
- Test error scenarios

### UI Tests
- Test critical user paths
- Test form validation
- Test navigation flows
- Test responsive design

## 📝 Commit Message Guidelines

Use conventional commit format:
```
type(scope): description

[optional body]

[optional footer]
```

### Types
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

### Examples
```
feat(validation): add comprehensive input validation
fix(database): resolve migration issue with existing data
docs(readme): update installation instructions
refactor(viewmodel): improve error handling
```

## 🚀 Pull Request Process

### Before Submitting
1. Ensure all tests pass
2. Update documentation if needed
3. Test database migrations
4. Perform manual testing
5. Self-review your code

### Pull Request Template
Use the provided PR template and fill out all relevant sections:
- Description of changes
- Type of change
- Testing completed
- Breaking changes (if any)
- Screenshots (if applicable)

### Review Process
1. Automated checks must pass
2. Code review by maintainers
3. Manual testing by reviewers
4. Documentation review
5. Final approval and merge

## 🐛 Bug Reports

### Bug Report Template
```
**Description**
Clear description of the bug

**Steps to Reproduce**
1. Step 1
2. Step 2
3. Step 3

**Expected Behavior**
What should happen

**Actual Behavior**
What actually happens

**Environment**
- OS: [e.g., Windows 11, macOS 14]
- Java Version: [e.g., 21.0.1]
- App Version: [e.g., 1.1.7]

**Additional Information**
Screenshots, logs, or other relevant information
```

## 💡 Feature Requests

### Feature Request Template
```
**Description**
Clear description of the feature

**Use Case**
Why this feature is needed

**Proposed Solution**
How you think it should work

**Alternative Solutions**
Other approaches considered

**Additional Information**
Mockups, examples, or other context
```

## 📚 Documentation

### Code Documentation
- Add KDoc comments for public APIs
- Document complex business logic
- Include examples for usage
- Update README for new features

### User Documentation
- Update user guides for new features
- Add screenshots for UI changes
- Provide migration guides for breaking changes
- Include troubleshooting sections

## 🔧 Development Tools

### Recommended IDE
- IntelliJ IDEA with Kotlin plugin
- Android Studio (for UI development)

### Useful Plugins
- Kotlin
- Compose Multiplatform
- SQLDelight
- Git Integration

### Build Commands
```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run the application
./gradlew run

# Create distribution
./gradlew packageReleaseMsi
```

## 🚨 Security Guidelines

### Data Validation
- Always validate user input
- Sanitize data before database storage
- Use parameterized queries
- Implement proper error handling

### Sensitive Information
- Never commit API keys or passwords
- Use environment variables for configuration
- Encrypt sensitive data in database
- Follow security best practices

## 🤝 Community Guidelines

### Communication
- Be respectful and inclusive
- Provide constructive feedback
- Help other contributors
- Follow the project's code of conduct

### Recognition
- Contributors will be credited in the project
- Significant contributions will be highlighted
- Maintainers will provide guidance and support

## 📞 Getting Help

### Questions and Support
- Create an issue for questions
- Join the project discussions
- Contact maintainers directly
- Check existing documentation

### Resources
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [Material Design 3](https://m3.material.io/)

---

Thank you for contributing to Merchant Center! Your contributions help make this project better for everyone. 