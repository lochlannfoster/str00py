# str00py Agent Configurations

## Recommended Agent Patterns

### 1. Codebase Exploration Agent

**When to use:** Understanding unfamiliar parts of the codebase

**Prompt pattern:**
```
Explore the [component] in /home/loch/str00py. I need to understand:
1. What files are involved
2. How data flows through the component
3. Key methods and their purposes
4. Any tests that exist
```

**Best for:**
- SessionManager flow
- Database layer
- UI/ViewModel interaction

### 2. Test Writing Agent

**When to use:** Adding test coverage

**Prompt pattern:**
```
Write tests for [component] in str00py following the existing test patterns.
Check app/src/test/java for unit tests and app/src/androidTest for instrumented tests.
Use Mockito for mocking, follow existing test structure.
```

### 3. Debugging Agent

**When to use:** Investigating session/lock issues

**Prompt pattern:**
```
Debug the [symptom] issue in str00py.
1. Trace the code path from StroopAccessibilityService
2. Check SessionManager state transitions
3. Look for race conditions or timing issues
4. Propose fix with test
```

### 4. Android Build Agent

**When to use:** Build issues, dependency problems

**Prompt pattern:**
```
Investigate build issue in str00py:
[error message]

Check:
- app/build.gradle.kts for dependencies
- settings.gradle.kts for project structure
- gradle.properties for configuration
```

## Agent Communication

Agents should report:
1. Files examined
2. Key findings
3. Recommendations with file paths and line numbers
4. Any blockers or questions

## Context to Provide Agents

Always include:
- Project path: `/home/loch/str00py`
- Relevant component name
- Specific symptom or goal
- Any error messages
