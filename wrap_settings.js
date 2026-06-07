const fs = require('fs');
const path = require('path');

const files = [
    'composeApp/src/androidMain/kotlin/app/it/fast4x/rimusic/ui/screens/settings/AIRecommendationSettings.kt',
    'composeApp/src/androidMain/kotlin/app/it/fast4x/rimusic/ui/screens/settings/AppearanceSettings.kt',
    'composeApp/src/androidMain/kotlin/app/it/fast4x/rimusic/ui/screens/settings/GeneralSettings.kt'
];

files.forEach(file => {
    const filePath = path.join(__dirname, file);
    if (!fs.existsSync(filePath)) return;
    
    let content = fs.readFileSync(filePath, 'utf8');

    // Regex to find OtherSwitchSettingEntry or OtherSettingsEntry calls that aren't inside an if statement starting with "if (search.inputValue.isBlank()"
    // We match the call and capture the 'title = ...' part to use it in the condition.
    
    // We need to carefully balance parentheses and braces to capture the full component block.
    // Instead of complex regex for balanced braces, we can process line by line.

    let lines = content.split('\n');
    let outputLines = [];
    let insideComponent = false;
    let componentType = '';
    let titleLine = '';
    let componentBuffer = [];
    
    for (let i = 0; i < lines.length; i++) {
        let line = lines[i];
        let trimmed = line.trim();
        
        if (!insideComponent) {
            if (trimmed.startsWith('OtherSwitchSettingEntry(') || trimmed.startsWith('OtherSettingsEntry(')) {
                // Check if it's already wrapped
                let prevLine = i > 0 ? lines[i-1].trim() : '';
                if (prevLine.startsWith('if (search.inputValue.isBlank()')) {
                    outputLines.push(line);
                    continue;
                }
                
                insideComponent = true;
                componentType = trimmed.split('(')[0];
                componentBuffer = [line];
                titleLine = '';
            } else {
                outputLines.push(line);
            }
        } else {
            componentBuffer.push(line);
            
            if (trimmed.startsWith('title = ')) {
                titleLine = trimmed.substring('title = '.length);
                if (titleLine.endsWith(',')) titleLine = titleLine.slice(0, -1);
            }
            
            // Check for end of component. A simple heuristic: if it's `)` or `) ` at the same indentation level as the start
            let startIndent = componentBuffer[0].match(/^\s*/)[0].length;
            let currentIndent = line.match(/^\s*/)[0].length;
            
            // A better heuristic is counting parentheses
            let openCount = 0;
            let text = componentBuffer.join('\n');
            for (let char of text) {
                if (char === '(') openCount++;
                if (char === ')') openCount--;
            }
            
            if (openCount === 0) {
                // Component is fully captured
                insideComponent = false;
                
                if (titleLine) {
                    let indent = componentBuffer[0].match(/^\s*/)[0];
                    let ifStmt = `${indent}if (search.inputValue.isBlank() || ${titleLine}.contains(search.inputValue, true)) {`;
                    outputLines.push(ifStmt);
                    componentBuffer.forEach(bLine => {
                        outputLines.push('    ' + bLine);
                    });
                    outputLines.push(`${indent}}`);
                } else {
                    // Fallback if no title found
                    componentBuffer.forEach(bLine => outputLines.push(bLine));
                }
            }
        }
    }
    
    fs.writeFileSync(filePath, outputLines.join('\n'), 'utf8');
    console.log(`Updated ${file}`);
});
