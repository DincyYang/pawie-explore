package pawie.oracle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal JSON reader and writer, just enough for the trace protocol.
 *
 * The oracle deliberately has no third-party dependencies: it has to build and
 * run with nothing but a JDK, both on a laptop and in CI.
 *
 * Numbers come back as Integer when they are integral, which keeps the
 * reference implementations working with the same value domain the visualizer
 * uses (whole numbers and short strings).
 */
public final class Json {

  private Json() { }

  /* ---------------- writing ---------------- */

  public static String write(Object v) {
    StringBuilder sb = new StringBuilder();
    writeTo(sb, v);
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  private static void writeTo(StringBuilder sb, Object v) {
    if (v == null) {
      sb.append("null");
    } else if (v instanceof String) {
      writeString(sb, (String) v);
    } else if (v instanceof Boolean || v instanceof Integer || v instanceof Long) {
      sb.append(v.toString());
    } else if (v instanceof Double || v instanceof Float) {
      double d = ((Number) v).doubleValue();
      if (d == Math.rint(d) && !Double.isInfinite(d)) sb.append((long) d);
      else sb.append(d);
    } else if (v instanceof Map) {
      sb.append('{');
      boolean first = true;
      for (Map.Entry<String, Object> e : ((Map<String, Object>) v).entrySet()) {
        if (!first) sb.append(',');
        first = false;
        writeString(sb, e.getKey());
        sb.append(':');
        writeTo(sb, e.getValue());
      }
      sb.append('}');
    } else if (v instanceof List) {
      sb.append('[');
      boolean first = true;
      for (Object o : (List<Object>) v) {
        if (!first) sb.append(',');
        first = false;
        writeTo(sb, o);
      }
      sb.append(']');
    } else {
      writeString(sb, v.toString());
    }
  }

  private static void writeString(StringBuilder sb, String s) {
    sb.append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"': sb.append("\\\""); break;
        case '\\': sb.append("\\\\"); break;
        case '\n': sb.append("\\n"); break;
        case '\r': sb.append("\\r"); break;
        case '\t': sb.append("\\t"); break;
        default:
          if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
          else sb.append(c);
      }
    }
    sb.append('"');
  }

  /* ---------------- reading ---------------- */

  public static Object parse(String src) {
    Parser p = new Parser(src);
    p.skipWs();
    Object v = p.value();
    p.skipWs();
    if (p.pos != src.length()) throw new IllegalArgumentException("trailing JSON at " + p.pos);
    return v;
  }

  private static final class Parser {
    private final String s;
    private int pos;

    Parser(String s) { this.s = s; }

    void skipWs() {
      while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
    }

    Object value() {
      skipWs();
      if (pos >= s.length()) throw new IllegalArgumentException("unexpected end of JSON");
      char c = s.charAt(pos);
      switch (c) {
        case '{': return object();
        case '[': return array();
        case '"': return string();
        case 't': expect("true"); return Boolean.TRUE;
        case 'f': expect("false"); return Boolean.FALSE;
        case 'n': expect("null"); return null;
        default: return number();
      }
    }

    private void expect(String word) {
      if (!s.startsWith(word, pos)) throw new IllegalArgumentException("bad literal at " + pos);
      pos += word.length();
    }

    Map<String, Object> object() {
      Map<String, Object> m = new LinkedHashMap<>();
      pos++;                      // consume '{'
      skipWs();
      if (pos < s.length() && s.charAt(pos) == '}') { pos++; return m; }
      while (true) {
        skipWs();
        String k = string();
        skipWs();
        if (s.charAt(pos) != ':') throw new IllegalArgumentException("expected ':' at " + pos);
        pos++;
        m.put(k, value());
        skipWs();
        char c = s.charAt(pos++);
        if (c == '}') return m;
        if (c != ',') throw new IllegalArgumentException("expected ',' or '}' at " + (pos - 1));
      }
    }

    List<Object> array() {
      List<Object> a = new ArrayList<>();
      pos++;                      // consume '['
      skipWs();
      if (pos < s.length() && s.charAt(pos) == ']') { pos++; return a; }
      while (true) {
        a.add(value());
        skipWs();
        char c = s.charAt(pos++);
        if (c == ']') return a;
        if (c != ',') throw new IllegalArgumentException("expected ',' or ']' at " + (pos - 1));
      }
    }

    String string() {
      if (s.charAt(pos) != '"') throw new IllegalArgumentException("expected string at " + pos);
      pos++;
      StringBuilder sb = new StringBuilder();
      while (true) {
        char c = s.charAt(pos++);
        if (c == '"') return sb.toString();
        if (c != '\\') { sb.append(c); continue; }
        char e = s.charAt(pos++);
        switch (e) {
          case '"': sb.append('"'); break;
          case '\\': sb.append('\\'); break;
          case '/': sb.append('/'); break;
          case 'b': sb.append('\b'); break;
          case 'f': sb.append('\f'); break;
          case 'n': sb.append('\n'); break;
          case 'r': sb.append('\r'); break;
          case 't': sb.append('\t'); break;
          case 'u':
            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
            pos += 4;
            break;
          default: throw new IllegalArgumentException("bad escape at " + (pos - 1));
        }
      }
    }

    Object number() {
      int start = pos;
      while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) pos++;
      String raw = s.substring(start, pos);
      double d = Double.parseDouble(raw);
      if (d == Math.rint(d) && Math.abs(d) < Integer.MAX_VALUE && raw.indexOf('.') < 0
          && raw.indexOf('e') < 0 && raw.indexOf('E') < 0) {
        return (int) d;
      }
      return d;
    }
  }
}
