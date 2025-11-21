/**
 * Script to update all HTML files to include config.js
 * Run this before deploying to ensure all pages use CONFIG
 */

const fs = require('fs');
const path = require('path');

const htmlFiles = [
    'frontend/HTML/index.html',
    'frontend/HTML/timKiem.html',
    'frontend/HTML/giamGia.html',
    'frontend/HTML/Trangchitiet.html',
    'frontend/HTML/danhMuc.html'
];

const configScriptTag = '<script src="../JS/config.js"></script>';

htmlFiles.forEach(filePath => {
    try {
        let content = fs.readFileSync(filePath, 'utf8');
        
        // Check if config.js already included
        if (content.includes('config.js')) {
            console.log(`✅ ${filePath} already has config.js`);
            return;
        }
        
        // Find where to insert (before websocket.js or first script tag)
        const websocketScriptPos = content.indexOf('<script src="../JS/websocket.js">');
        if (websocketScriptPos !== -1) {
            // Insert before websocket.js
            content = content.slice(0, websocketScriptPos) + 
                     configScriptTag + '\n    ' + 
                     content.slice(websocketScriptPos);
        } else {
            // Insert before closing </body>
            const bodyClosePos = content.lastIndexOf('</body>');
            if (bodyClosePos !== -1) {
                content = content.slice(0, bodyClosePos) + 
                         '    ' + configScriptTag + '\n' +
                         content.slice(bodyClosePos);
            }
        }
        
        fs.writeFileSync(filePath, content, 'utf8');
        console.log(`✅ Updated ${filePath}`);
        
    } catch (error) {
        console.error(`❌ Error updating ${filePath}:`, error.message);
    }
});

console.log('\n✨ All HTML files updated successfully!');
