/* ***************************************************************
 * Autor............: Fabricio da Silva Souza
 * Matricula........: 202411217
 * Inicio...........: 02/07/2026
 * Ultima alteracao.: 03/07/2026
 * Nome.............: Apdu
 * Funcao...........: Encodes and decodes application protocol data.
 *************************************************************** */

package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class Apdu {

  private final String type;
  private final List<String> params;

  private Apdu(String type, List<String> params) {
    this.type = type.toUpperCase(Locale.ROOT);
    this.params = Collections.unmodifiableList(new ArrayList<String>(params));
  }

  public static Apdu of(String type, String... params) {
    return new Apdu(type, Arrays.asList(params));
  }

  public String getType() {
    return type;
  }

  public List<String> getParams() {
    return params;
  }

  public String param(int index) {
    return params.get(index);
  }

  public int size() {
    return params.size();
  }

  public String encode() {
    StringBuilder encoded = new StringBuilder(type).append('(');
    for (int i = 0; i < params.size(); i++) {
      if (i > 0) {
        encoded.append(',');
      }
      encoded.append(escape(params.get(i)));
    }
    return encoded.append(')').toString();
  }

  public static Apdu decode(String raw) {
    if (raw == null) {
      throw new IllegalArgumentException("Empty APDU.");
    }

    String text = raw.trim();
    int open = text.indexOf('(');
    if (open <= 0 || !text.endsWith(")")) {
      throw new IllegalArgumentException("Invalid APDU: " + raw);
    }

    String type = text.substring(0, open).trim();
    String body = text.substring(open + 1, text.length() - 1);
    List<String> params = parseParams(body);
    return new Apdu(type, params);
  }

  private static List<String> parseParams(String body) {
    List<String> params = new ArrayList<String>();
    if (body.length() == 0) {
      return params;
    }

    StringBuilder current = new StringBuilder();
    boolean escaped = false;
    for (int i = 0; i < body.length(); i++) {
      char c = body.charAt(i);
      if (escaped) {
        if (c == 'n') {
          current.append('\n');
        } else if (c == 'r') {
          current.append('\r');
        } else {
          current.append(c);
        }
        escaped = false;
      } else if (c == '\\') {
        escaped = true;
      } else if (c == ',') {
        params.add(current.toString());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }

    if (escaped) {
      current.append('\\');
    }
    params.add(current.toString());
    return params;
  }

  private static String escape(String value) {
    StringBuilder escaped = new StringBuilder();
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '\\' || c == ',' || c == '(' || c == ')') {
        escaped.append('\\').append(c);
      } else if (c == '\n') {
        escaped.append("\\n");
      } else if (c == '\r') {
        escaped.append("\\r");
      } else {
        escaped.append(c);
      }
    }
    return escaped.toString();
  }

  @Override
  public String toString() {
    return encode();
  }
}
