const fs = require('fs');

console.log('Testing TTS API with corrected file handling...');
console.log('API key available:', !!process.env.OPENAI_API_KEY);

// Use dynamic import for ES module
import('ai-text-to-speech').then(tts => {
  console.log('TTS module loaded successfully');

  const aiSpeech = tts.default;
  return aiSpeech({
    input: 'This is a corrected test of the TTS system that should create a real MP3 file',
    voice: 'nova',
    model: 'tts-1',
    response_format: 'mp3'
  });
}).then(ttsResultPath => {
  console.log('✅ TTS API returned path:', ttsResultPath);

  // Check if the returned path exists and is a real audio file
  if (fs.existsSync(ttsResultPath)) {
    const stats = fs.statSync(ttsResultPath);
    console.log('  Original file size:', stats.size, 'bytes');

    // Copy to our target location (simulating the Joyride behavior)
    const targetPath = '/Users/pez/Projects/Meetup/joydrive-lm-tool-prezo/slides/voice/hello-node-test.mp3';
    fs.copyFileSync(ttsResultPath, targetPath);

    const targetStats = fs.statSync(targetPath);
    console.log('  Copied to:', targetPath);
    console.log('  Target file size:', targetStats.size, 'bytes');

    // Clean up the original file
    fs.unlinkSync(ttsResultPath);
    console.log('  Cleaned up original file');

    // Verify the file type
    require('child_process').exec(`file "${targetPath}"`, (error, stdout) => {
      if (error) {
        console.error('File type check error:', error.message);
      } else {
        console.log('  File type:', stdout.trim());
      }
    });

  } else {
    console.error('❌ TTS result path does not exist:', ttsResultPath);
  }
}).catch(error => {
  console.error('TTS Error:', error.message);
  console.error('Full error:', error);
});
