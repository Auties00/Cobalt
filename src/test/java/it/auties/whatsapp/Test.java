package it.auties.whatsapp;

import it.auties.whatsapp.util.ConcurrentDoublyLinkedList;

import java.util.Objects;

public class Test {
    public static void main(String[] args) {
        var data = new ConcurrentDoublyLinkedList<>();
        data.add("abc");
        System.out.println(Objects.hashCode(data));
        data.add("def");
        System.out.println(Objects.hashCode(data));
    }
}
