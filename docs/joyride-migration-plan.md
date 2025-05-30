# Joyride Presentation Migration Plan

## Current State Analysis

The current slide deck is about "Backseat Driver" - a VS Code extension that provides AI tools for Interactive Programming with Clojure. The presentation needs to be migrated to focus on **Joyride** and its new **LM Tool integration feature**.

## MVP Migration Requirements

### 1. Update Title Slide (`slides/hello.md`)
**MUST DO:**
- Change title from "Backseat Driver" to "Joyride: AI Hacking VS Code with Interactive Programming"
- Update subtitle to mention LM Tool integration
- Keep personal info but update the project focus

### 2. Update Topics Overview (`slides/topics.md`)
**MUST DO:**
- Replace "Backseat Driver" topics with Joyride-focused agenda:
  - What is Joyride?
  - The new LM Tool integration feature
  - Interactive Programming with the REPL
  - AI Hacking VS Code in user space
  - Demo: Building with Joyride + AI
  - Experience and learnings
  - Where next? (MCP and beyond)

### 3. Replace Backseat Driver Intro (`slides/backseat-driver.md`)
**MUST DO:**
- Create new slide: "Joyride - What is it?"
- Focus on:
  - VS Code scripting with ClojureScript
  - User space automation and customization
  - REPL-driven development in VS Code
  - NEW: LM Tool integration for AI assistance

### 4. Update Why Section (`slides/why.md`)
**MUST DO:**
- Retitle: "Why Joyride + AI Tools?"
- Focus on:
  - Interactive Programming in VS Code
  - ClojureScript for VS Code automation
  - AI assistance with VS Code API access
  - Live coding and REPL feedback
  - Remove Backseat Driver references

### 5. Remove Tools Section (`slides/tools.md`)
**REMOVE:**
- Merge any essential content into the REPL slide
- The REPL slide will cover the primary tool (`joyride_evaluate_code`)

### 6. Update REPL Section (`slides/repl.md`)
**MUST DO:**
- Retitle: "The Joyride LM Tool: `joyride_evaluate_code`"
- Focus on Joyride's REPL capabilities
- Demonstrate the `joyride_evaluate_code` tool
- Include any relevant content from the tools slide
- Show safety mechanisms (if any)
- Interactive development workflow
- **NEW IMAGE NEEDED:** Screenshot of the tool in action

### 7. Remove Interactive Programming Instructions (`slides/interactive-programming-instructions.md`)
**REMOVE:**
- Content too detailed for presentation format
- Core concepts covered in other slides

### 8. Move Inline Defs to Addendum (`slides/inline-defs.md`)
**MOVE TO END:**
- Keep as bonus/advanced content
- Place in addendum section after main presentation

### 9. Update Experience Section (`slides/so-far.md`)
**MUST DO:**
- Retitle: "Joyride + AI: Early Experiences"
- Update content to reflect Joyride + LM Tool experiences
- Keep general AI programming challenges
- Add Joyride-specific successes and challenges

### 10. Update Ecosystem Section (`slides/alternatives.md`)
**MUST DO:**
- Retitle: "Joyride + Complementary Extensions"
- Content focus:
  - **Calva**: Essential for Clojure development in VS Code
  - **Backseat Driver**: Can connect to Joyride REPL for additional AI tools
  - **Integration note**: When using Backseat Driver with Joyride, disable Joyride's eval tool
  - **Recommended Workflow**: Use Calva for app development REPL, Joyride for VS Code automation
- **NEW IMAGE NEEDED:** Architecture diagram showing Calva + Joyride + Backseat Driver integration

### 11. Keep Thanks Section (`slides/thanks.md`)
**NO CHANGES NEEDED:**
- Personal contact info remains relevant

### 12. Update Slides Configuration (`slides.edn`)
**MUST DO:**
- Remove `"slides/tools.md"`
- Remove `"slides/interactive-programming-instructions.md"`
- Move `"slides/inline-defs.md"` to addendum section
- Update slide order for better flow

## Updated Slides Configuration

### 🔧 **New `slides.edn` Structure:**

```clojure
{:slides ["slides/hello.md",
          "slides/topics.md"
          "slides/what-is-joyride.md"    ; Replaces backseat-driver.md
          "slides/why-joyride-ai.md"     ; Updated why.md
          "slides/joyride-lm-tool.md"    ; Updated repl.md + tools content
          "slides/experiences.md"        ; Updated so-far.md
          "slides/ecosystem.md"          ; Updated alternatives.md
          "slides/thanks.md"
          ;; == ADDENDUM ==
          "slides/addendum-inline-defs.md"]}  ; Moved from main deck
```

### 📝 **File Rename Actions Needed:**
- `backseat-driver.md` → `what-is-joyride.md`
- `why.md` → `why-joyride-ai.md`
- `repl.md` → `joyride-lm-tool.md`
- `so-far.md` → `experiences.md`
- `alternatives.md` → `ecosystem.md`
- `inline-defs.md` → `addendum-inline-defs.md`

### 🗑️ **Files to Remove:**
- `slides/tools.md` (content merged into `joyride-lm-tool.md`)
- `slides/interactive-programming-instructions.md` (too detailed for presentation)

---
## Implementation Priority

### Phase 1: Essential Content Migration
1. **Title slide** - First impression matters
2. **Topics slide** - Set correct expectations
3. **What is Joyride** - Replace Backseat Driver intro
4. **Why Joyride + AI** - Core value proposition
5. **Update slides.edn** - Remove/reorder slides

### Phase 2: Technical Content
6. **REPL section** - Demonstrate capabilities (merge tools content)
7. **Experience section** - Real-world learnings
8. **Ecosystem section** - Calva + Backseat Driver integration

### Phase 3: Structure & Polish
9. **Move inline defs to addendum**
10. **Remove interactive programming instructions**
11. **Gather/create required images**

## Key Messaging Focus

**Primary Message:** Joyride now enables AI agents to hack VS Code in user space through Interactive Programming

**Supporting Points:**
- ClojureScript + VS Code API = Powerful automation
- LM Tool integration brings AI assistance to VS Code scripting
- Interactive Programming methodology for AI-assisted development
- Live demos of building VS Code features with AI help

## Demo Preparation Notes

- Prepare examples of using `joyride_evaluate_code` tool
- Show AI building actual VS Code automations
- Demonstrate REPL-driven development with AI assistance
- Have backup demos ready for live presentation

## Technical Requirements

- Update slide deck infrastructure (already present with `next-slide` script)
- Ensure demo environment is set up with Joyride + LM tools
- Test all examples and demos before presentation
- Have fallback content for technical difficulties

## Required Images & Assets

### 🎨 **New Images Needed:**

#### 1. **Joyride Logo/Icon**
- **Location needed:** Title slide, topics slide
- **Purpose:** Replace Backseat Driver branding
- **Format:** SVG preferred, high-res PNG acceptable
- **Style:** Should fit with existing Clojure/VS Code aesthetic

#### 2. **Joyride LM Tool Screenshot**
- **Location needed:** REPL slide (`slides/repl.md`)
- **Purpose:** Show `joyride_evaluate_code` tool in action
- **Content:** VS Code with Joyride eval tool being used by AI
- **Size:** Should fit in slide layout (approx 600-800px wide)

#### 3. **Architecture Diagram**
- **Location needed:** Ecosystem slide (`slides/alternatives.md`)
- **Purpose:** Show Calva + Joyride + Backseat Driver integration
- **Content:**
  - Calva ↔ App REPL (Clojure development)
  - Joyride ↔ VS Code API (Editor automation)
  - Backseat Driver ↔ Joyride REPL (AI assistance)
- **Style:** Clean, technical diagram

#### 4. **Joyride + ClojureScript Screenshot**
- **Location needed:** "What is Joyride" slide
- **Purpose:** Show Joyride script in action
- **Content:** VS Code with a simple Joyride script being executed
- **Focus:** Highlight ClojureScript nature and VS Code integration

### 🔄 **Images to Update/Replace:**

#### 1. **Icon Gallery (topics slide)**
- Replace `backseat-driver-icon.png` with Joyride icon
- Keep `clj.png` and `copilot-icon-light.png`
- Consider adding VS Code icon to show the integration

#### 2. **Header Images**
- Replace `backseat-driver-header.png` with Joyride equivalent
- Or create new header showing "Joyride + AI" theme

### 📁 **Existing Images to Keep:**
- `agical.svg` - Company logo
- `pappapez.png` - Personal photo
- `clj.png` - Clojure logo
- `copilot-icon-light.png` - GitHub Copilot icon
- `vscode.png` - VS Code logo (if used)

### 🎯 **Image Creation Priority:**
1. **HIGH:** Joyride logo/icon (needed for multiple slides)
2. **HIGH:** LM Tool screenshot (core demo content)
3. **MEDIUM:** Architecture diagram (technical explanation)
4. **LOW:** Additional screenshots (nice-to-have)

---

**Next Steps:** Start with Phase 1 content migration, focusing on the core narrative shift from Backseat Driver to Joyride + LM Tool integration.
