/* Comparing two step traces.
 *
 * Both engines snapshot the state their own fields describe, so while a resize
 * is under way, or during a step that only narrates, the state simply repeats.
 * Runs of identical consecutive states are collapsed on both sides before the
 * comparison, which leaves exactly the moments where observable state changed.
 * Those have to line up one for one. */

export const canonical = (v) => JSON.stringify(v);

export function collapse(states) {
  const out = [];
  for (const s of states) {
    const key = canonical(s);
    if (out.length && out[out.length - 1].key === key) continue;
    out.push({ key, value: s });
  }
  return out;
}

function describe(value) {
  const text = canonical(value);
  return text.length > 220 ? text.slice(0, 217) + '...' : text;
}

/**
 * Compares one operation. Returns a list of human-readable differences, empty
 * when the two engines agree.
 */
export function diffStep(engineStep, javaStep, opts = {}) {
  const problems = [];
  const where = `${javaStep.name}(${javaStep.args.map(describe).join(', ')})`;
  const project = (s) => {
    if (!opts.stateKeys) return s;
    const out = {};
    for (const k of opts.stateKeys) out[k] = s[k];
    return out;
  };

  const engineFailed = engineStep.error !== null && engineStep.error !== false;
  const javaFailed = javaStep.error !== null;
  if (engineFailed !== javaFailed) {
    problems.push(
      `${where}: the visualizer ${engineFailed ? 'refused the call' : 'accepted the call'} `
      + `but the reference ${javaFailed ? 'threw ' + javaStep.error : 'accepted it'}`,
    );
    return problems;                    // the traces are not comparable after this
  }

  const a = collapse(engineStep.states.map(project));
  const b = collapse(javaStep.states.map(project));
  const n = Math.max(a.length, b.length);
  for (let i = 0; i < n; i++) {
    const left = a[i];
    const right = b[i];
    if (!left) {
      problems.push(`${where}: the visualizer stopped after ${a.length} state(s); `
        + `the reference went on to ${describe(right.value)}`);
      break;
    }
    if (!right) {
      problems.push(`${where}: the visualizer showed an extra state ${describe(left.value)} `
        + `after the reference had finished`);
      break;
    }
    if (left.key !== right.key) {
      problems.push(`${where}: state ${i} differs\n`
        + `      visualizer: ${describe(left.value)}\n`
        + `      reference : ${describe(right.value)}`);
      break;                            // one mismatch is enough to report
    }
  }

  if (canonical(engineStep.finalState) !== canonical(javaStep.finalState)) {
    problems.push(`${where}: the committed state afterwards differs\n`
      + `      visualizer: ${describe(engineStep.finalState)}\n`
      + `      reference : ${describe(javaStep.finalState)}`);
  }

  if (!opts.skipCost) {
    for (const key of Object.keys(javaStep.cost)) {
      const mine = engineStep.cost[key] ?? 0;
      const theirs = javaStep.cost[key];
      if (mine !== theirs) {
        problems.push(`${where}: counted ${mine} ${key}, the reference counted ${theirs}`);
      }
    }
  }

  return problems;
}

export function diffRun(engineSteps, javaSteps, opts = {}) {
  const problems = [];
  const n = Math.min(engineSteps.length, javaSteps.length);
  for (let i = 0; i < n; i++) {
    problems.push(...diffStep(engineSteps[i], javaSteps[i], opts).map((p) => `step ${i + 1}: ${p}`));
    if (problems.length) break;         // stop at the first divergence: the rest cascades
  }
  return problems;
}
