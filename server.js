// Pawie tutor proxy.
//
// A tiny local server that holds your ANTHROPIC_API_KEY and proxies the mascot's
// chat box to the Claude API, so the key never reaches the browser.
//
// Curated RAG: for each data structure we inject the matching course material
// (assignment write-up, teaching notes) into the system prompt so answers stay
// grounded in the actual course. Those files are NOT part of this repository —
// see `course-context/README.md`. Without them Pawie still works and answers
// from general data-structures knowledge.
//
// Run:
//   export ANTHROPIC_API_KEY=sk-ant-...      # never commit your key
//   node server.js
//
// Then serve the page from the same folder and open explore.html:
//   python3 -m http.server 8123

import { createServer } from "node:http";
import { readFile, readdir } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import Anthropic from "@anthropic-ai/sdk";

const PORT = Number(process.env.PAWIE_PORT) || 8787;
const MODEL = process.env.PAWIE_MODEL || "claude-haiku-4-5"; // fast + cheap, ideal for short tutoring replies
const ROOT = dirname(fileURLToPath(import.meta.url));
const CONTEXT_DIR = join(ROOT, "course-context");

if (!process.env.ANTHROPIC_API_KEY) {
  console.error("\n  ✗ ANTHROPIC_API_KEY is not set.");
  console.error("    Run:  export ANTHROPIC_API_KEY=sk-ant-...  &&  node server.js\n");
  process.exit(1);
}
const client = new Anthropic(); // reads ANTHROPIC_API_KEY from the environment

// Optional per-structure grounding material. Drop your own files into
// course-context/<Structure>/ — e.g. course-context/ArrayList/writeup.md — and
// they are injected into that structure's system prompt. Missing folders are
// simply skipped.
const MAX_CHARS = 7000; // cap each source so the context stays lean
const contextCache = new Map(); // structure -> assembled context string

async function loadContext(structure) {
  if (contextCache.has(structure)) return contextCache.get(structure);
  const parts = [];
  try {
    const dir = join(CONTEXT_DIR, structure);
    for (const name of await readdir(dir)) {
      if (!/\.(md|txt)$/i.test(name)) continue;
      let text = await readFile(join(dir, name), "utf8");
      if (text.length > MAX_CHARS) text = text.slice(0, MAX_CHARS) + "\n…(truncated)";
      parts.push(`### Source: ${name}\n${text}`);
    }
  } catch {
    /* no course-context/<structure> folder — fine, we fall back to general knowledge */
  }
  const ctx = parts.join("\n\n---\n\n") || "(no course material supplied for this structure)";
  contextCache.set(structure, ctx);
  return ctx;
}

function systemPrompt(structure, context) {
  return (
    `You are Pawie, a friendly brown pixel-bear tutor living in the corner of an ` +
    `interactive data-structure visualizer for an introductory data structures course (Java). ` +
    `The student is currently looking at the ${structure} visualizer.\n\n` +
    `How to help:\n` +
    `- Keep replies SHORT — 2–4 sentences, plain and encouraging. You are a quick companion, not a lecture.\n` +
    `- Be Socratic when it helps: nudge the student toward the idea instead of just stating it.\n` +
    `- Always tie answers to what they can see: indices, pointers, front/rear, capacity vs size, the Big-O cost.\n` +
    `- Ground your answers in the course context below. If the question is outside it, answer from general data-structures knowledge and say so briefly.\n` +
    `- NEVER write a full solution to a graded programming assignment. Explain concepts and give tiny illustrative snippets only.\n` +
    `- If you don't know, say so.\n\n` +
    `=== Course context for ${structure} ===\n${context}`
  );
}

function send(res, status, body) {
  res.writeHead(status, {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
  });
  res.end(JSON.stringify(body));
}

const server = createServer((req, res) => {
  if (req.method === "OPTIONS") return send(res, 204, {});
  if (req.method !== "POST" || !req.url.startsWith("/api/tutor")) {
    return send(res, 404, { error: "Not found. POST to /api/tutor." });
  }

  let raw = "";
  req.on("data", (c) => {
    raw += c;
    if (raw.length > 1e6) req.destroy(); // basic guard against huge bodies
  });
  req.on("end", async () => {
    try {
      const { structure = "ArrayList", question = "", history = [] } = JSON.parse(raw || "{}");
      if (!question.trim()) return send(res, 400, { error: "Empty question." });

      const context = await loadContext(structure);
      // Keep only the last few turns to bound tokens; coerce to the API shape.
      const turns = (Array.isArray(history) ? history.slice(-6) : [])
        .filter((m) => m && (m.role === "user" || m.role === "assistant") && m.content)
        .map((m) => ({ role: m.role, content: String(m.content) }));

      const response = await client.messages.create({
        model: MODEL,
        max_tokens: 800,
        system: [
          {
            type: "text",
            text: systemPrompt(structure, context),
            cache_control: { type: "ephemeral" }, // stable per-structure prefix → cache it
          },
        ],
        messages: [...turns, { role: "user", content: question }],
      });

      const answer = response.content
        .filter((b) => b.type === "text")
        .map((b) => b.text)
        .join("")
        .trim();

      send(res, 200, { answer: answer || "Hmm, I went blank — try asking again?" });
    } catch (err) {
      console.error("tutor error:", err?.message || err);
      const status = err?.status && Number.isInteger(err.status) ? err.status : 500;
      const friendly =
        status === 401 ? "Invalid ANTHROPIC_API_KEY — restart the server with a valid key."
        : status === 429 ? "Rate limited — wait a moment and try again."
        : err?.message || "Tutor request failed.";
      send(res, status, { error: friendly });
    }
  });
});

server.listen(PORT, () => {
  console.log(`\n  🐻 Pawie tutor proxy running at http://localhost:${PORT}/api/tutor`);
  console.log(`     model: ${MODEL}   (Ctrl+C to stop)\n`);
});
