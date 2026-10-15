'use strict';
const fs = require('node:fs');
const crypto = require('node:crypto');

// Format: magic (8), salt (16), IV (12), GCM tag (16), ciphertext.
const MAGIC = Buffer.from('SLDBENC1');
function decryptBackup(encrypted, password) {
  if (!password) throw new Error('Set DB_BACKUP_PASSWORD before decrypting.');
  if (encrypted.length < 52 || !encrypted.subarray(0, 8).equals(MAGIC)) {
    throw new Error('Unsupported encrypted database backup.');
  }
  const salt = encrypted.subarray(8, 24);
  const iv = encrypted.subarray(24, 36);
  const key = crypto.scryptSync(password, salt, 32, { N: 32768, r: 8, p: 1, maxmem: 64 * 1024 * 1024 });
  try {
    const decipher = crypto.createDecipheriv('aes-256-gcm', key, iv);
    decipher.setAAD(encrypted.subarray(0, 36));
    decipher.setAuthTag(encrypted.subarray(36, 52));
    return Buffer.concat([decipher.update(encrypted.subarray(52)), decipher.final()]);
  } finally { key.fill(0); }
}

module.exports = { decryptBackup };
if (require.main === module) {
  let plaintext;
  try {
    const [input, output] = process.argv.slice(2);
    if (!input || !output) throw new Error('Usage: node Decrypt-DatabaseBackup.cjs INPUT.sql.enc OUTPUT.sql');
    plaintext = decryptBackup(fs.readFileSync(input), process.env.DB_BACKUP_PASSWORD);
    fs.writeFileSync(output, plaintext, { flag: 'wx', mode: 0o600 });
    console.log('Database backup decrypted successfully.');
  } catch (error) {
    console.error(error.code === 'EEXIST' ? 'Output already exists; choose a new output filename.' : 'Decryption failed: check the password, input file, and output path.');
    process.exitCode = 1;
  } finally { plaintext?.fill(0); }
}
