# Pawie · Explore

An interactive visualizer for the core data structures of an introductory
data-structures course, with **Pawie**, a pixel-bear tutor powered by the
Claude API, living in the corner of the page.

Every operation is animated step by step: you watch indices move, pointers
rewire, capacity double, and the cost of each step accumulate. No Java source
is shown; the narration is plain English, so the focus stays on *what the
structure does*, not on reading code.

**Open `explore.html` in a browser and it just runs**: no server, no API key,
no build step. The Claude-powered chat is an optional extra on top.

## What's in it

**Eight structures**, each with the real constructors and operations from the
course assignments:

| Structure | What you can watch |
| --- | --- |
| ArrayList | shifting on insert/remove, size vs capacity, amortized growth |
| LinkedList | pointer surgery on insert/remove, walking to an index |
| Iterator | a cursor sitting *between* nodes, O(1) stepping, `remove()` rules |
| Stack | LIFO push/pop on a deque backing |
| Queue | FIFO enqueue/dequeue on a circular array, wrap-around |
| HashMap / HashSet | hashing, bucket placement, collision handling, resize |
| Heap | percolate up/down on a tree view plus its backing array |
| BST | search paths, insert, the three delete cases |

**Pawie the tutor** answers open questions about whatever you are looking at.
The page sends the current structure and the conversation so far to a small
local proxy (`server.js`), which calls Claude with a system prompt grounded in
that structure's course material. Pawie also tells jokes, cheers you on, and
quizzes you, and that content is curated in `pawie-content.js`, no model involved.

## Running it

### Just the visualizer, no setup at all

Clone or download this repo and **open `explore.html` in your browser.** That's
it. No server, no API key, no build step. All eight structures, every animation
and every step-by-step explanation work offline, as does Pawie's curated
jokes / cheers / quiz content.

This is the whole visualizer. The rest of this section is optional.

### Adding the tutor chat

To also *ask Pawie questions*, run the small proxy that holds your API key so it
never reaches the browser. You need Node.js 18+ and an
[Anthropic API key](https://console.anthropic.com).

```bash
npm install
```

**Terminal 1 (the tutor proxy):**

```bash
export ANTHROPIC_API_KEY=sk-ant-your-key-here
node server.js
```

**Terminal 2 (serve the page)** (the chat needs a real origin, so open it over
http rather than as a local file):

```bash
python3 -m http.server 8123
```

Then open <http://localhost:8123/explore.html>.

Without the proxy the chat box simply says Pawie's tutor brain is offline;
nothing else on the page is affected.

### Optional: grounding Pawie in your own course

`server.js` looks for `course-context/<Structure>/*.md` and injects those files
into the system prompt for that structure. Create the folder yourself:

```
course-context/
  ArrayList/
    writeup.md
    common-misconceptions.md
  Heap/
    writeup.md
```

It is gitignored on purpose: course write-ups, solutions and teaching notes
belong to their authors and should not be redistributed. Without it Pawie still
works and answers from general data-structures knowledge.

## Layout

```
explore.html       the whole visualizer: structures, animation, transport, Pawie UI
pawie-content.js   curated jokes, cheers and quiz banks (edit copy here)
server.js          local proxy to the Claude API; the only place the key lives
```

## Notes

- The key is read from the environment and stays on the server. Never paste it
  into a file you might commit.
- Pawie is told never to write a full solution to a graded assignment.
