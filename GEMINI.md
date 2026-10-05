# CineGrade Agent Guidelines

## AI-Generated Files & Methods Tagging Policy
1. **Source Code / Resource Tagging**:
   - Whenever creating or generating files, UI resources (FXML, CSS), or new methods/code sections, tag them at the top (or directly above the method/section):
     - Java full file: `// [AI-GENERATED]`
     - Java partial/method: `// [AI-GENERATED METHOD]` or `// [AI-GENERATED TABLE STRINGS]`
     - FXML: `<!-- [AI-GENERATED] -->` (placed directly below the `<?xml ...?>` declaration, without modifying import statements)
     - CSS: `/* [AI-GENERATED] */` (placed at line 1)
2. **Tracking in AI_GENERATED.md**:
   - Every time a new FXML, CSS file, or AI-implemented method/file is created, record it in `AI_GENERATED.md` at the project root with the file path, component type, and description.
3. **Import Preservation**:
   - Never modify or alter import statements in code files unless explicitly instructed to do so.
