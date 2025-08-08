package it.auties.whatsapp.socket.stream;

import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.socket.SocketConnection;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

abstract sealed class NodeHandler {
    protected final SocketConnection socketConnection;
    private final Set<String> descriptions;

    private NodeHandler(SocketConnection socketConnection, String... descriptions) {
        this.socketConnection = socketConnection;
        this.descriptions = Set.of(descriptions);
    }

    Set<String> descriptions() {
        return descriptions;
    }

    abstract void handle(Node node);
    abstract void dispose();

    non-sealed abstract static class Executor extends NodeHandler {
        private final ExecutorService service;

        protected Executor(SocketConnection socketConnection, String... descriptions) {
            super(socketConnection, descriptions);
            this.service = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
        }

        abstract void execute(Node node);

        @Override
        final void handle(Node node) {
            service.execute(() -> execute(node));
        }

        @Override
        final void dispose() {
            service.shutdownNow();
        }
    }

    non-sealed abstract static class Dispatcher extends NodeHandler {
        protected Dispatcher(SocketConnection socketConnection, String... descriptions) {
            super(socketConnection, descriptions);
        }

        abstract void execute(Node node);

        @Override
        final void handle(Node node) {
            Thread.startVirtualThread(() -> execute(node));
        }

        @Override
        void dispose() {

        }
    }
}
