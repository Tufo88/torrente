package utils;

import java.io.Serializable;

public record Pair<K, V>(K key, V value) implements Serializable {

}
