# CareerPilot Project Scope

This document outlines the master scope of CareerPilot. It begins by listing all possible capabilities, then organizes them into phases: MVP, V1, V2, and Advanced.

## Classification Principles

- **MVP**: Sufficient to demonstrate three genuinely different features working end-to-end.
- **V1**: Improves AI and UX quality without changing the system's fundamental nature.
- **V2**: Begins to demonstrate more sophisticated AI engineering.
- **Advanced**: Interesting features that could easily turn a personal project into a large platform.

## 1. Overall Scope

```
CareerPilot
├── Shared AI Infrastructure
├── 1. AI Chat
│   ├── Conversation
│   ├── User Context
│   ├── User Instructions
│   ├── Scope Control
│   ├── Context Management
│   └── External Knowledge
├── 2. CV Analysis
│   ├── CV Review
│   └── CV and JD Analysis
└── 3. Mock Interview
    ├── Interview Setup
    ├── Question Generation
    ├── Answer Evaluation
    ├── Adaptive Interview
    ├── Final Assessment
    └── Fresh Knowledge
```

## 2. Shared AI Infrastructure

This section covers functionality shared across all three features.

### 2.1 Gemini Integration

#### MVP
- Gemini API integration
- Centralized AI service
- Model configuration
- Temperature and generation configuration
- API error handling
- Timeout handling
- Basic rate-limit handling

#### V1
- Streaming
- Retry with limits
- Better fallback and error messages
- Token usage tracking

#### V2
- Model abstraction
- Ability to switch models without modifying business logic
- Model selection per task

#### Advanced
- Multi-model routing
- Model cost optimization
- Automatic model selection

## 3. Prompt Management

This is a section we strongly want in the project.

### MVP

Organize prompts by workflow:

- chat-system
- cv-review
- cv-jd-analysis
- interview-question
- interview-evaluation
- interview-final-report

Avoid hard-coding prompts scattered throughout Java code.

### V1

Prompt variables:

- {{user_profile}}
- {{career_goal}}
- {{conversation_context}}
- {{cv}}
- {{jd}}
- {{role}}
- {{difficulty}}

Prompt versioning:

- cv-review-v1
- cv-review-v2

### V2
- Prompt template management
- Prompt experiments
- Prompt evaluation
- A/B testing prompts

### Advanced
- Automatic prompt optimization
- Prompt performance analytics

**MVP Recommendation**: Prompt templates with variables are sufficient.

## 4. Context Management

This is one of the most important parts of the project.

### MVP

Distinguish between:

- System Context
- User Context
- Conversation Context
- Feature Context

Current conversations are saved.

### V1
- Context window management
- Limit number of messages sent to the model
- Recent-message prioritization
- Conversation summarization when conversations get too long

### V2
- Semantic conversation retrieval
- Relevant message retrieval
- Long-term user memory
- Cross-conversation memory with selectivity

### Advanced
- Automatic memory extraction
- Memory importance scoring
- Memory conflict resolution

## 5. User Instructions and Metadata

This is the capability you recently proposed.

### MVP

Users can set:

- Preferred language
- Response style
- Technical background
- Career goal
- Custom instructions

These instructions apply to all conversations.

### V1
- Edit and delete instructions
- Preview instruction context
- Validate instruction length
- Sanitize and classify instructions

### V2
- Structured user profile
- Automatically update profile from conversations
- User controls which memories are saved

### Advanced
- Memory extraction
- Conflict resolution between old and new user information
- Memory lifecycle

Example:

User says: "I mainly use Java."

Later: "I have moved to Python."

The system must recognize that new information may supersede old information.

## 6. Scope and Safety

This capability should exist from MVP, though initial implementation can be simple.

### MVP

Chat has a defined scope:

- Career
- Software Engineering
- Learning
- CV
- JD
- Interview
- AI

Out-of-scope queries are refused or redirected.

Additionally:

- Do not reveal system prompt
- Do not treat user instructions as system instructions
- Do not treat CV, JD, or web content as instructions
- Do not fabricate context about the user

### V1
- Intent classification
- Prompt injection detection
- Input validation
- Output validation

### V2
- Dedicated guard model
- Risk classification
- More granular policies

### Advanced
- Adversarial testing framework
- Automated prompt injection evaluation

## 7. Web Search Infrastructure

Not necessarily required for MVP of the entire system.

### MVP

Not needed yet.

### V1
- Search API abstraction
- Retrieve relevant sources
- Inject search results into AI context
- Source attribution

### V2
- Freshness detection
- Query rewriting
- Source ranking
- Deduplication
- Citation
- Domain filtering

### Advanced
- Multi-step research
- Search to read to search again
- Research agent

## 8. AI Chat

Now we reach the main features.

### A. Basic Conversation

#### MVP
- Create conversation
- Send message
- Receive response
- Save history
- Load conversation
- Delete conversation
- Conversation title

#### V1
- Streaming response
- Regenerate response
- Edit user message
- Retry failed response
- Markdown rendering

#### V2
- Message branching
- Conversation search
- Conversation summary
- Archive

#### Advanced
- Cross-conversation retrieval
- Automatic memory extraction
- Conversation insights

## 9. Chat Context

### MVP

Chat uses:

```
System Prompt
+
User Instructions
+
Current Conversation
+
User Profile
```

### V1

Additionally include:

- Relevant CV Analysis results (explicit and selective, not everything)
- Relevant Interview results

### V2

Web Search:

```
User question
  ↓
Need fresh knowledge?
  ↓
Search
  ↓
Context
  ↓
Gemini
```

### Advanced
- Cross-conversation semantic retrieval
- Personal knowledge base
- RAG

## 10. Chat Scope Intelligence

### MVP

Prompt-based scope control.

### V1

Intent classification:

- GENERAL_CAREER
- TECHNICAL
- CV_DISCUSSION
- INTERVIEW_DISCUSSION
- OUT_OF_SCOPE

### V2

Intent with context requirements:

```json
{
  "intent": "CAREER_SPECIFIC",
  "requiresProfile": true,
  "requiresCv": false,
  "requiresWeb": false
}
```

### Advanced

Dynamic context orchestration.

## 11. CV Analysis - CV Review

This is the first workflow.

### Input

#### MVP
- PDF CV

#### V1
- DOCX
- TXT
- Better file validation

#### V2
- Multiple CV versions
- Image and scanned CV with OCR

#### Advanced
- Complex document layouts
- Multilingual CV parsing

## 12. CV Content Extraction

#### MVP

```
PDF → Text extraction → Gemini
```

#### V1

Parse into structured representation:

```
Candidate
├── Personal
├── Summary
├── Education
├── Experience
├── Projects
├── Skills
└── Certifications
```

#### V2
- Section detection
- Better handling of malformed documents
- Extraction confidence scores

#### Advanced
- Layout-aware parsing
- OCR
- Vision model

## 13. CV Review AI

#### MVP

Output:

- Overall assessment
- Strengths
- Weaknesses
- Recommendations

#### V1

Structured output:

- Summary
- Experience
- Projects
- Skills
- Education
- Achievements

For each section:

- Score
- Issues
- Recommendations

#### V2
- Quantification detection
- Impact analysis
- Weak bullet detection
- Redundancy detection
- ATS-oriented analysis

#### Advanced
- Rewrite suggestions
- Multiple rewrite styles
- Target-role optimization

## 14. CV and JD Analysis

This is the second workflow.

### MVP

**Input**:

- CV
- JD

**Output**:

- Match Score
- Matched Skills
- Missing Skills
- Experience Gaps
- Recommendations

### V1

Analyze requirements by:

- Required
- Preferred
- Optional

And by:

- Technical Skills
- Experience
- Education
- Domain
- Soft Skills

### V2
- Requirement prioritization
- Evidence mapping

Example:

```
JD requires: Kafka

CV evidence:
Project X → Kafka

Confidence: High
```

This is a valuable feature.

### Advanced
- ATS simulation
- Resume optimization
- Automatic CV tailoring

## 15. Mock Interview - Setup

### MVP

User selects:

- Role
- Level
- Topic
- Number of questions
- Difficulty

Example:

- Java Backend
- Middle
- Java + Spring
- 5 questions
- Medium

### V1
- Interview duration
- Topic weighting
- Difficulty progression
- Interview type (Technical, Behavioral, Mixed)

### V2
- JD-based interview

```
JD → Extract requirements → Generate interview
```

### Advanced
- Company-specific interview
- Resume-based interview

## 16. Mock Interview - Question Generation

### MVP

Gemini generates questions based on:

- Role
- Level
- Topic
- Difficulty

### V1

Structured question:

```json
{
  "question": "...",
  "topic": "Spring",
  "difficulty": "Medium",
  "expectedConcepts": []
}
```

### V2
- Question diversity
- Avoid duplicates
- Difficulty calibration
- Question dependency

### Advanced

Dynamic interviewer:

- User answers well → harder question
- User answers poorly → follow-up to verify understanding

## 17. Mock Interview - Answer Evaluation

This is one of the most important features.

### MVP

Evaluate:

- Correctness
- Technical depth
- Completeness

Output:

- Score
- Strengths
- Weaknesses
- Feedback

### V1

Structured evaluation:

- Score
- Correct concepts
- Missing concepts
- Incorrect concepts
- Communication
- Recommendation

### V2
- Role-specific rubric
- Difficulty-aware scoring
- Compare against expected answer
- Follow-up question generation

### Advanced
- Communication analysis
- Filler words detection
- Speech analysis
- Voice interview

## 18. Mock Interview - Interview State Machine

### MVP

```
SETUP
  ↓
QUESTION
  ↓
ANSWER
  ↓
EVALUATION
  ↓
NEXT QUESTION
  ↓
...
  ↓
COMPLETED
```

Application maintains state.

### V1
- Pause and resume
- Skip question
- Retry answer

### V2

Adaptive interview:

```
Good answer → Harder question

Weak answer → Follow-up
```

### Advanced

Fully dynamic interviewer.

## 19. Mock Interview - Final Report

### MVP
- Overall Score
- Strengths
- Weaknesses
- Recommendations

### V1

Score breakdown:

```
Java        8/10
Spring      6/10
SQL         7/10
```

### V2

Personalized learning plan:

```
Weakness
  ↓
Recommended topic
  ↓
Learning priority
```

### Advanced

Track performance across multiple interviews.

## 20. Mock Interview - Web Search

This is the section you recently added.

### MVP

Assumption: Web search is not required for every interview.

Only support interviews based on stable knowledge.

### V1

Web search for:

- Version-specific topics
- Current technologies
- Current frameworks
- Current industry practices

Examples:

- Spring Boot 4
- Java 25
- Current AWS services
- Current AI frameworks

### V2

Knowledge preparation:

```
Interview config
  ↓
Identify fresh topics
  ↓
Search authoritative sources
  ↓
Build knowledge context
  ↓
Generate questions
```

### Advanced

Dynamic research during interview.

Example: Model detects "This depends on Spring Boot version." → search → verify → continue.

We will be cautious at this level due to significant complexity increase.

## 21. Cross-feature Integration

This is very interesting but should not be in MVP.

### V1

CV Analysis to Chat:

- Discuss my CV analysis.

Mock Interview to Chat:

- Explain why I scored poorly.

### V2

CV to Interview:

```
CV → Skills and target role → Interview configuration
```

JD to Interview:

```
JD → Requirements → Interview topics
```

### Advanced

Full career loop:

```
CV
  ↓
CV Analysis
  ↓
Skill Gap
  ↓
Learning
  ↓
Mock Interview
  ↓
Evaluation
  ↓
Chat
  ↓
Improve CV
```

This could become CareerPilot's long-term vision.

## 22. Evaluation and Observability

This section is not directly visible to users but is extremely valuable if you want the project to demonstrate AI engineering.

### MVP
- Log requests and errors
- Track model failures

### V1

Save:

- Prompt
- Model
- Response
- Latency
- Token usage

Not necessary to save sensitive user data indefinitely.

### V2

AI evaluation:

```
Response
  ↓
Quality criteria
  ↓
Score
```

Track:

- Relevance
- Hallucination
- Instruction following
- Structured output validity

### Advanced
- Golden datasets
- Regression tests
- Prompt evaluation pipeline
- Automated LLM evaluation

This is one of the sections we highly value for CV purposes, but it should not be included in MVP.

## 23. Persistence and Backend

Not an AI capability but necessary for features to work.

### MVP

Entities:

- User
- Conversation
- Message
- CV
- CVAnalysis
- InterviewSession
- InterviewQuestion
- InterviewAnswer
- InterviewEvaluation

Database: PostgreSQL

### V1
- Soft delete
- Pagination
- Better indexing
- File metadata
- Analysis history

### V2
- Search
- Versioning
- Audit trail

## 24. Authentication

If the goal is a CV project, authentication should not be overly complex.

### MVP

Either:

- Single-user or development account
- Basic authentication

### V1
- Login
- Register
- Session and JWT
- User-specific data isolation

### V2
- OAuth2
- Google and GitHub login

## 25. UI and UX

### MVP

Three main areas:

**Chat**
- Conversation list
- Message area
- Input

**CV**
- Upload
- Analysis mode
- Result

**Interview**
- Setup
- Question
- Answer
- Evaluation
- Progress

### V1
- Streaming
- Better loading states
- Error states
- Markdown
- Progress indicators

### V2
- Charts
- Interactive analysis
- Interview history
- Compare results

## 26. Security

### MVP
- API key not exposed to frontend
- File size limit
- File type validation
- Basic input validation

### V1
- Prompt injection mitigation
- File content validation
- Rate limiting
- User data isolation

### V2
- Malware scanning
- Content moderation
- Secure file storage
- Encryption considerations

### Advanced
- Comprehensive AI security evaluation

## 27. Roadmap Summary

This is the most important section.

## 27.1 Green MVP - "CareerPilot works"

### Shared
- Gemini integration
- Centralized AI service
- Prompt templates
- Basic error handling
- PostgreSQL persistence

### Chat
- Create conversation
- Conversation history
- Text-only chat
- User instructions
- User profile context
- Career-focused system prompt
- Basic out-of-scope handling

### CV Analysis
- PDF upload
- CV text extraction
- CV Review
- Structured analysis output
- CV and JD Analysis
- Structured match result

### Mock Interview
- Interview setup
- Question generation
- User answer
- Answer evaluation
- Next question
- Interview state
- Final report
- Stable-knowledge interview

MVP stops here.

## 27.2 Yellow V1 - "CareerPilot becomes good"

### Shared
- Streaming
- Token and context management
- Prompt variables
- Better retry and error handling
- Token usage tracking

### Chat
- Conversation summary
- Better intent classification
- Selective feature context
- Regenerate response
- Conversation management

### CV
- DOCX support
- Structured CV extraction
- Section-level scoring
- Requirement categorization
- Evidence mapping

### Interview
- Structured questions
- Better evaluation rubric
- Adaptive difficulty
- Follow-up questions
- Interview configuration from JD

### Web
- Web search abstraction
- Source retrieval
- Source attribution
- Fresh and version-specific interview knowledge

## 27.3 Orange V2 - "CareerPilot becomes an AI application"

### Context
- Long-term memory
- Cross-conversation retrieval
- Semantic memory
- Selective context retrieval

### Chat
- Web search
- Dynamic context selection
- More sophisticated intent routing

### CV
- CV version comparison
- CV tailoring
- Skill gap analysis
- ATS-oriented analysis

### Interview
- Adaptive interview
- Resume-based interview
- JD-based interview
- Performance tracking across sessions

### AI Engineering
- Tool calling
- Output validation
- AI evaluation
- Prompt versioning
- Model abstraction

## 27.4 Red Advanced - "Should not be done if the goal is only a CV project"

These can be done but we should actively prevent scope creep here:

- Full RAG
- Vector database
- Autonomous agent
- Multi-agent interview
- Voice interview
- Speech-to-text
- Pronunciation analysis
- Facial and body-language analysis
- Automatic job searching
- Automatic job application
- Company research agent
- Multi-model orchestration
- Automatic prompt optimization
- Full AI evaluation platform
- Real-time collaborative interview
- Complex authentication and authorization
- Microservices

These do not make CareerPilot better proportional to the complexity they add.

## 28. Capability Prioritization

Not every feature has the same value.

### Highly Recommended
- Structured Output
- Context Management
- User Instructions
- Conversation Memory
- CV Analysis
- CV and JD Matching
- Mock Interview State Machine
- Answer Evaluation
- Web Search and Fresh Knowledge
- Prompt Management

### Recommended Later
- Adaptive Interview
- Cross-feature context
- Long-term memory
- Tool Calling
- AI evaluation
- Prompt versioning

### Not Priority
- RAG
- Agents
- Voice
- Multi-agent

## 29. Recommended MVP Scope

If we had to freeze scope today, we would freeze at this point.

```
                    CAREERPILOT MVP
                          |
             |------------|-----------|
             |            |           |
           CHAT       CV ANALYSIS   MOCK INTERVIEW
             |            |           |
        |--------|    |--------|  |---------|
        |        |    |        |  |         |
    Conversation User CV Review CV+JD  Interview Evaluation
      Memory     Info  Analysis  Match    Flow
        |        |
        |--------|
             |
          Gemini
```

Web Search is not a required feature for MVP, but it is designed as a shared capability that Mock Interview can use in V1.

The most important point is: MVP does not need to prove CareerPilot has every AI capability. It needs to prove three workflows have different natures, but are built on a well-designed AI infrastructure.

For your CV purposes, we believe this scope is well-balanced: small enough to actually complete, but substantive enough that you can later discuss prompt engineering, context engineering, structured output, stateful AI workflow, retrieval, and evaluation without needing to "add" technology just for show.
