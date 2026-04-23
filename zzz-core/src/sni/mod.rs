use anyhow::{anyhow, Result};
use bytes::Bytes;

/// Inject (or replace) the SNI hostname in a TLS ClientHello record.
///
/// Layout of a TLS record containing ClientHello:
///   [0]      = 0x16  (handshake)
///   [1..2]   = version (0x03 0x01 etc.)
///   [3..4]   = record length
///   [5]      = 0x01  (client_hello)
///   [6..8]   = handshake length (3 bytes)
///   [9..10]  = client_version
///   [11..42] = random (32 bytes)
///   [43]     = session_id length
///   ...
///   extensions area includes server_name (type 0x0000)
pub fn inject_sni(payload: Bytes, new_host: &str) -> Result<Vec<u8>> {
    let mut buf = payload.to_vec();

    // Verify TLS handshake record
    if buf.len() < 43 || buf[0] != 0x16 || buf[5] != 0x01 {
        return Ok(buf); // not a ClientHello, pass through
    }

    // Walk to extensions
    let mut pos = 43usize;

    // skip session_id
    if pos >= buf.len() { return Ok(buf); }
    let sid_len = buf[pos] as usize;
    pos += 1 + sid_len;

    // skip cipher_suites
    if pos + 2 > buf.len() { return Ok(buf); }
    let cs_len = u16::from_be_bytes([buf[pos], buf[pos+1]]) as usize;
    pos += 2 + cs_len;

    // skip compression methods
    if pos + 1 > buf.len() { return Ok(buf); }
    let comp_len = buf[pos] as usize;
    pos += 1 + comp_len;

    // extensions length
    if pos + 2 > buf.len() { return Ok(buf); }
    let _ext_len = u16::from_be_bytes([buf[pos], buf[pos+1]]) as usize;
    pos += 2;

    let ext_start = pos;

    // Search for SNI extension (type 0x0000)
    while pos + 4 <= buf.len() {
        let ext_type = u16::from_be_bytes([buf[pos], buf[pos+1]]);
        let ext_data_len = u16::from_be_bytes([buf[pos+2], buf[pos+3]]) as usize;

        if ext_type == 0x0000 {
            // Found SNI extension — replace hostname in place or rebuild
            return replace_sni_extension(&mut buf, pos, ext_data_len + 4, new_host);
        }
        pos += 4 + ext_data_len;
    }

    // No SNI extension found — inject one
    inject_new_sni_extension(&mut buf, ext_start, new_host)
}

fn replace_sni_extension(buf: &mut Vec<u8>, ext_offset: usize, old_ext_len: usize, new_host: &str) -> Result<Vec<u8>> {
    let new_ext = build_sni_extension(new_host);
    let mut result = Vec::with_capacity(buf.len() - old_ext_len + new_ext.len());
    result.extend_from_slice(&buf[..ext_offset]);
    result.extend_from_slice(&new_ext);
    result.extend_from_slice(&buf[ext_offset + old_ext_len..]);

    fix_lengths(&mut result);
    Ok(result)
}

fn inject_new_sni_extension(buf: &mut Vec<u8>, ext_start: usize, new_host: &str) -> Result<Vec<u8>> {
    let new_ext = build_sni_extension(new_host);
    let mut result = Vec::with_capacity(buf.len() + new_ext.len());
    result.extend_from_slice(&buf[..ext_start]);
    result.extend_from_slice(&new_ext);
    result.extend_from_slice(&buf[ext_start..]);

    fix_lengths(&mut result);
    Ok(result)
}

fn build_sni_extension(host: &str) -> Vec<u8> {
    let host_bytes = host.as_bytes();
    let name_len = host_bytes.len();
    // server_name_list_len (2) + name_type (1) + name_len (2) + name
    let list_len = 1 + 2 + name_len;
    // ext_type (2) + ext_data_len (2) + server_name_list_len (2) + ...
    let ext_data_len = 2 + list_len;

    let mut ext = Vec::with_capacity(4 + ext_data_len);
    ext.push(0x00); ext.push(0x00); // extension type: server_name
    ext.extend_from_slice(&(ext_data_len as u16).to_be_bytes());
    ext.extend_from_slice(&(list_len as u16).to_be_bytes());
    ext.push(0x00); // name_type: host_name
    ext.extend_from_slice(&(name_len as u16).to_be_bytes());
    ext.extend_from_slice(host_bytes);
    ext
}

/// Fix TLS record length and ClientHello handshake length after modification
fn fix_lengths(buf: &mut Vec<u8>) {
    if buf.len() < 9 { return; }

    // TLS record length = total - 5 (header)
    let record_len = (buf.len() - 5) as u16;
    buf[3] = (record_len >> 8) as u8;
    buf[4] = (record_len & 0xFF) as u8;

    // Handshake length (3 bytes at offset 6) = total - 9
    let hs_len = buf.len() - 9;
    buf[6] = ((hs_len >> 16) & 0xFF) as u8;
    buf[7] = ((hs_len >> 8) & 0xFF) as u8;
    buf[8] = (hs_len & 0xFF) as u8;

    // Extensions total length: walk to extension block
    if buf.len() < 44 { return; }
    let mut pos = 43usize;
    if pos >= buf.len() { return; }
    let sid_len = buf[pos] as usize; pos += 1 + sid_len;
    if pos + 2 > buf.len() { return; }
    let cs_len = u16::from_be_bytes([buf[pos], buf[pos+1]]) as usize; pos += 2 + cs_len;
    if pos >= buf.len() { return; }
    let comp_len = buf[pos] as usize; pos += 1 + comp_len;
    if pos + 2 > buf.len() { return; }

    // Fix extension block length
    let ext_block_len = (buf.len() - pos - 2) as u16;
    buf[pos] = (ext_block_len >> 8) as u8;
    buf[pos + 1] = (ext_block_len & 0xFF) as u8;
}
