const generateCodeVerifier = async () => {
    const e = new Uint8Array(32);  // 生成 32 字节（256 位）的随机数组
    window.crypto.getRandomValues(e);  // 使用加密安全的随机数填充数组
    return Array.from(e, s => s.toString(16).padStart(2, "0")).join("");  // 转为十六进制字符串
};
const generateCodeChallenge = async (e) => {
    const a = new TextEncoder().encode(e);  // 将 `code_verifier` 转换为 UTF-8 字节
    const i = await crypto.subtle.digest("SHA-256", a);  // 计算 SHA-256 哈希
    const c = Array.from(new Uint8Array(i));  // 转换为字节数组
    return btoa(String.fromCharCode.apply(null, c))  // Base64 编码
        .replace(/\+/g, "-")  // URL 安全编码（`+` → `-`）
        .replace(/\//g, "_")  // URL 安全编码（`/` → `_`）
        .replace(/=/g, "");   // 去掉 `=` 填充
};
(async () => {
    const codeVerifier = await generateCodeVerifier();
    const codeChallenge = await generateCodeChallenge(codeVerifier);

    console.log("Code Verifier:", codeVerifier);
    console.log("Code Challenge:", codeChallenge);
})();
