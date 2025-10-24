# Project Build & Dev Tools

Command-line tool for managing development projects with AI template support.

## Why This Tool?

This tool addresses common pain points in multi-project development environments:

- **Simplified Project Navigation**: Quickly open projects across multiple organizations without remembering paths
- **Consistent Git Workflow**: Clone repositories to the correct directory structure automatically
- **AI-Friendly Project Management**: Built-in support for Project Requirement Prompts (PRPs) to work seamlessly with AI coding assistants
- **Terminal Integration**: Change directory in the current terminal after project selection (via batch wrapper)

## About the PRP Convention

The PRP (Project Requirement Prompts) convention is based on videos and discussions by Cole Medin (GitHub: [coleam00](https://github.com/coleam00), YouTube: [@ColeMedin](https://www.youtube.com/@ColeMedin)). See his video: [Project Requirement Prompts](https://www.youtube.com/watch?v=Mk87sFlUG28).

This is my implementation and might differ from Cole's approach. I don't completely follow his workflow—the PRP thought process made sense to me, and I use it daily. So much so that I made this simple tool. I neither endorse nor promote his videos/content, but the PRP concept is due credit to him and his ecosystem, which is why I'm mentioning it. I'm also providing this reference so people understand why this PRP convention exists, what problems it solves, and how to use PRPs effectively.

I have my own reasons for using PRPs, but to highlight one key benefit: it's better not to have status in the same file as instructions—it gets messy quickly and consumes more tokens. Having read-only PRPs and AI-only status files keeps things clear. Using multiple files instead of one file with all project requirements is also beneficial. It makes it possible to finish one feature before hitting context limits. Even if you exceed limits, the status report helps in resuming after compaction or clearing the conversation. These small benefits add up. I felt it was too much to explain here, so I'm pointing you to Cole's video. His conventions and commentary might differ from mine—my approach is independently inspired, but he and his ecosystem are the source.

## Requirements

- Java 11 or higher
- Windows, Linux, or macOS
- Maven (for building from source)

## Installation

### Option 1: Download Pre-built Release

1. Download the latest release ZIP from the `releases/` folder
2. Extract `proj.bat` and `xyz-jphil-ai-proj_build_dev_misc_tools-1.0.jar`
3. Place them in a directory of your choice
4. Run `proj.bat` (Windows)

### Option 2: Build from Source

1. Clone this repository
2. Run the release build script:
   ```bash
   make-release.bat
   ```
   This will:
   - Build the project with Maven
   - Create a timestamped release ZIP in `releases/` folder
   - Package both `proj.bat` and the shaded JAR

3. Extract the ZIP and use `proj.bat` to run the tool with directory-change support

## Usage

Run the tool using:
```bash
proj.bat
```

Or directly with Java:
```bash
java -jar xyz-jphil-ai-proj_build_dev_misc_tools-1.0.jar
```

### First Run

On first run, the tool creates a settings file at:
```
<user-home>/xyz-jphil/ai/proj-build_dev_misc_tools/Settings.xml
```

Edit this file to configure:
- Code repositories base path
- Default text editor
- NetBeans IDE path
- Organizations with their Maven groupIds

### Main Features

**1. Open Project**
- List projects from configured organizations
- Sort alphabetically or by recent modification
- Open in: current terminal (with dir change), new CMD window, file explorer, NetBeans, or default editor

**2. Open Project (All Organizations)**
- Discover and open projects from any organization in your code repos directory
- Useful for exploring projects outside your configured list

**3. Git Clone Repository**
- Automatically places cloned repositories in the correct directory structure
- Format: `<code-repos>/<organization>/<repository-name>`
- Creates organization directories as needed

**4. Create New Project**
- Interactive wizard for new project creation
- Suggests Maven groupId based on organization
- Creates directory structure ready for initialization

**5. AI Template Management**
- Create and manage Project Requirement Prompts (PRPs)
- View PRP details and status directly in the terminal
- Open PRPs in your editor
- Close/reopen PRPs to track implementation status

**6. Settings Management**
- Edit settings without manually opening the XML file
- Add/edit/remove organizations
- Configure paths and tools

## Settings.xml Structure

```xml
<Settings>
    <CodeReposPath>C:\Users\User\code</CodeReposPath>
    <NetbeansPath>C:\Program Files\NetBeans\bin\netbeans.exe</NetbeansPath>
    <DefaultEditor>notepad.exe</DefaultEditor>

    <Organizations>
        <Organization>
            <Name>xyz-jphil</Name>
            <GroupId>io.github.xyz-jphil</GroupId>
        </Organization>
    </Organizations>

    <PrpTemplate><![CDATA[PRP Number: %index%
PRP Name: %name%
Usage Guide:
    - This is a Project Requirement Prompt (PRP). This file contains AI prompts that are intended to define certain requirement(s) for this project.
    - Claude Code (or any other coding AI agents) will be working on implementing this requirement in this project when told by the user. For AI coding agents this file is READ-ONLY and MUST NOT be modified by AI coding agents.
    - Once this PRP is completed (or temporarily stalled), it is renamed to `%index%-prp-%name%.closed.md`.
    - The file `%index%-prp.status.md` carries the status update for this prp, which is to be written/updated by the AI coding agents working on this prp.
]]></PrpTemplate>
</Settings>
```

### Key Configuration Options

- **CodeReposPath**: Base directory where all repositories are stored
- **NetbeansPath**: Full path to NetBeans executable (optional)
- **DefaultEditor**: Command to open text files (e.g., `notepad.exe`, `vim`, `code`)
- **Organizations**: List of GitHub organizations with their Maven groupIds
- **PrpTemplate**: Template for generating new PRP files (supports %index% and %name% placeholders)

## Directory Structure

Expected project layout:
```
<CodeReposPath>/
├── organization-1/
│   ├── project-1/
│   ├── project-2/
│   └── ...
├── organization-2/
│   ├── project-a/
│   ├── project-b/
│   └── ...
└── ...
```

## License

This project is intended for personal or organizational use in development workflows.
