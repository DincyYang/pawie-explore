// Pawie interaction copy: jokes, cheers, and quiz banks.
//
// This is the single place to edit or grow Pawie's entertainment content.
// explore.html loads this file BEFORE the mascot script and reads
// window.PawieContent. All content is English, curated (no API), and safe to
// ship to students. Add as many items as you like. Pawie picks at random.
//
// Style rule: do NOT use em dashes or semicolons in any copy. Use periods and
// commas instead.
//
// QUIZ keys must match the visualizer's structures: ArrayList, LinkedList,
// Iterator, Stack, Queue (plus a "default" pool used elsewhere). Each question:
//   { q: "...", opt: ["a","b","c"], ans: <index of correct option>, why: "..." }

window.PawieContent = {

  // jokes
  JOKES: [
    "Why was the stack so polite? It always lets the last one in leave first. 🃏",
    "I had a joke about a linked list, but you have to follow it all the way to the end. ➡️",
    "Why did the array go to therapy? Too many index issues. 📇",
    "A queue walks into a bar. The first one orders, and the rest wait their turn. 🍺",
    "Binary trees never feel lost. They always know their roots. 🌳",
    "Hash tables throw the best parties. Everyone gets a spot instantly. 🎉",
    "O(1) and O(n) raced. O(1) didn't even warm up. 🏃",
    "I'd explain recursion, but first I'd have to explain recursion. 🔁",
    "Why are pointers bad at dating? They keep referencing their ex. 💔",
    "Two arrays met and just clicked. Turns out they were on the same index. 😅",
    "My recursion joke kept going until it hit a stack overflow. 😵",
    "Why don't hash functions gossip? They can't reverse what they heard. 🤫",
    "The array and the linked list argued over who's faster. Honestly? It depends on the operation. 🤷",
    "Big-O strolled in and dropped all his constants. Nobody noticed. 🎤",
    "A stack and a queue opened a cafe. The stack served the newest customer first. Total chaos. ☕",
    "Why was binary search so calm? It just cut every problem in half. ✂️",
    "The tree asked the graph, 'Why so many cycles?' The graph said, 'I keep my options open.' 🔄",
    "A null pointer walked into a bar. There was no bar. 🫥",
    "The pointer proposed with a ring buffer. She said yes, it always comes back around. 💍",
    "Why did the queue feel unappreciated? Everyone kept cutting to the front of its cousin, the stack. 😤",
    "Amortized analysis is just doing your taxes for algorithms. 🧾",
    "The bug wasn't in the code. It was in my assumptions. Classic. 🐛"
  ],

  // cheers
  CHEERS: [
    "You're getting the hang of this! 🐾",
    "Getting stuck is part of learning, so keep poking at it. 💪",
    "Nice exploring! Curiosity is the best debugger. 🔍",
    "Every expert started by clicking buttons, just like you. ✨",
    "Take a breath. You've totally got this. 🌟",
    "Data structures click eventually. Today looks like a good day. 😄",
    "Small steps add up. You're doing great. 🚀",
    "Bugs aren't failures. They're clues, so follow them. 🔎",
    "You tried something new. That's exactly how it starts. 👏",
    "Slow is smooth, smooth is fast. Keep going. 🐢",
    "Confused now, clear later. That's the whole deal. 💡",
    "Love the question-asking energy today. 🙌",
    "Indices, pointers, indices again. You're wiring your brain. 🧠",
    "One more operation and it'll click. I promise. 🐻",
    "Rest your eyes a second, then come back sharp. 👀",
    "Whatever you're stuck on, past you couldn't do it either. That's progress. 📈"
  ],

  // quiz
  QUIZ: {
    ArrayList: [
      { q: "The array is full and you append one more. What happens?",
        opt: ["It throws an error", "Capacity doubles, then it's added", "It overwrites index 0"], ans: 1,
        why: "expandCapacity makes the new capacity old times 2, then copies the elements over." },
      { q: "What's the cost of get(i) on an ArrayList?",
        opt: ["O(1)", "O(n)", "O(log n)"], ans: 0,
        why: "Arrays give random access. One jump lands straight on index i, whatever the size." },
      { q: "Why is add(0, e) O(n)?",
        opt: ["It searches for a free spot", "It shifts every element right", "It doubles the array"], ans: 1,
        why: "Inserting at the front pushes all existing elements one slot to the right." },
      { q: "Which is cheaper: remove(0) or remove(size-1)?",
        opt: ["remove(0)", "remove(size-1)", "Exactly the same"], ans: 1,
        why: "Removing the last element shifts nothing, so O(1). Removing the first shifts everyone left, so O(n)." },
      { q: "What does capacity tell you that size doesn't?",
        opt: ["The Big-O of the list", "How many slots the array has, some maybe empty", "How many elements are stored"], ans: 1,
        why: "size is the count of elements stored. capacity is the total slots allocated, some possibly free." },
      { q: "Appending is usually O(1). Why do we say amortized?",
        opt: ["It's secretly always O(n)", "The rare doubling-copy averages out over many appends", "The compiler optimizes it"], ans: 1,
        why: "Most appends are O(1). The occasional resize copy spreads out to O(1) on average." }
    ],
    LinkedList: [
      { q: "How do you reach index i in a linked list?",
        opt: ["Jump straight there", "Walk i+1 links from the head", "Binary search"], ans: 1,
        why: "There is no indexing. You follow next pointers one by one, and that walk is the O(n) part." },
      { q: "Once you're at the right node, an insertion is...",
        opt: ["O(1), a few pointer swaps", "O(n), shift everything", "O(log n)"], ans: 0,
        why: "The splice itself just rewires a couple of pointers." },
      { q: "What does the head reference point to?",
        opt: ["The last node", "The first node", "A random node"], ans: 1,
        why: "head points to the first node, and you follow next from there." },
      { q: "Removing the head node is...",
        opt: ["O(1)", "O(n)", "O(log n)"], ans: 0,
        why: "You just repoint head to head.next, no walking required." },
      { q: "Both use nodes, so why is get(i) O(n) but a stack's push O(1)?",
        opt: ["push always works at a known end", "linked lists run on slower hardware", "get uses recursion"], ans: 0,
        why: "push and pop touch a known end. get(i) must walk to an arbitrary position." }
    ],
    Iterator: [
      { q: "Why are next() and previous() O(1)?",
        opt: ["The cursor sits between nodes", "It caches the whole list", "It uses an index"], ans: 0,
        why: "The cursor already knows where it is, so there is no walking from the head." },
      { q: "Before calling remove(), you must first call...",
        opt: ["nothing", "next() or previous()", "add()"], ans: 1,
        why: "remove() acts on the last node the iterator handed back." },
      { q: "Where does the iterator's cursor sit?",
        opt: ["On a node", "Between two nodes", "At the head only"], ans: 1,
        why: "It sits between nodes, so it can step either direction in O(1)." },
      { q: "Why loop with an iterator instead of get(i)?",
        opt: ["It's shorter to type", "It avoids re-walking from the head each time", "It uses less memory"], ans: 1,
        why: "get(i) in a loop is O(n squared). The iterator walks once, so O(n) total." }
    ],
    Stack: [
      { q: "Push 1, 2, 3, then pop, pop. What comes out?",
        opt: ["1 then 2", "3 then 2", "2 then 3"], ans: 1,
        why: "LIFO means Last In, First Out. The most recent push pops first." },
      { q: "What's the cost of push and pop?",
        opt: ["O(1)", "O(n)", "O(log n)"], ans: 0,
        why: "Both work at the top of the stack, so constant time (push amortized)." },
      { q: "Which real task fits a stack best?",
        opt: ["Undo history", "A printer job line", "A to-do list by priority"], ans: 0,
        why: "Undo is LIFO. The most recent action is undone first." },
      { q: "What does peek() do?",
        opt: ["Removes the top", "Returns the top without removing it", "Empties the stack"], ans: 1,
        why: "peek reads the top element but leaves it in place." },
      { q: "Underneath, this stack is a deque. push equals which call?",
        opt: ["addFirst", "addLast", "insert in the middle"], ans: 0,
        why: "push is addFirst (top of stack), and pop is removeFirst." }
    ],
    Queue: [
      { q: "Enqueue 1, 2, 3, then dequeue. What comes out?",
        opt: ["3", "1", "2"], ans: 1,
        why: "FIFO means First In, First Out. The earliest arrival leaves first." },
      { q: "Why can front end up greater than rear?",
        opt: ["It's a bug", "The indices wrap around the array", "The array shrank"], ans: 1,
        why: "It's a circular array, so indices wrap around to reuse freed slots." },
      { q: "Which real task fits a queue best?",
        opt: ["Undo history", "A printer job line", "A function call stack"], ans: 1,
        why: "Jobs are served in arrival order, which is FIFO." },
      { q: "enqueue adds to the ___, dequeue removes from the ___.",
        opt: ["front, back", "back, front", "back, back"], ans: 1,
        why: "Enqueue joins the back. Dequeue leaves from the front." },
      { q: "Why back a queue with a circular array?",
        opt: ["To reuse freed front slots without shifting", "To keep it sorted", "To save on element type"], ans: 0,
        why: "Wrapping lets front and rear cycle, so you don't shift everything on each dequeue." }
    ],
    default: [
      { q: "Big-O notation measures what?",
        opt: ["How fast your laptop is", "How the work grows as the input grows", "Lines of code"], ans: 1,
        why: "It describes how the work scales with n, the size of the data." },
      { q: "O(1) vs O(n): which grows with input size?",
        opt: ["O(1)", "O(n)", "Neither"], ans: 1,
        why: "O(1) is constant. O(n) grows linearly with n." },
      { q: "What does amortized O(1) mean?",
        opt: ["Always exactly one step", "The average over many operations is constant", "One step in the worst case"], ans: 1,
        why: "Occasional expensive steps average out to constant over many operations." }
    ]
  }
};
