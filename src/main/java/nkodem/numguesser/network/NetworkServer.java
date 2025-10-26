package nkodem.numguesser.network;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkServer {
    private final int port;
    private final int low;
    private final int high;
    private final int maxPlayers;
    private static final AtomicBoolean rankingSent = new AtomicBoolean(false);

    public NetworkServer(int port, int low, int high, int maxPlayers) {
        this.port = port;
        this.low = low;
        this.high = high;
        this.maxPlayers = Math.max(1, maxPlayers);
    }

    public void start() throws IOException {
        start(new Scanner(System.in));
    }

    public void start(Scanner consoleScanner) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        List<Thread> handlers = Collections.synchronizedList(new ArrayList<>());
        List<SessionInfo> sessions = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger nextId = new AtomicInteger(1);

        System.out.println("Waiting for up to " + maxPlayers + " players on port " + port + "...");

        HostController hostController = new HostController(sessions, handlers, serverSocket, consoleScanner, low, high);
        Thread hostThread = new Thread(hostController, "Host-Controller");
        hostThread.setDaemon(true);
        hostThread.start();
        System.out.println("Host console available. Type commands in server console.");


        try {
            for (int i = 0; i < maxPlayers; i++) {
                try {
                    Socket client = serverSocket.accept();
                    System.out.println("Client connected: " + client.getRemoteSocketAddress());

                    int id = nextId.getAndIncrement();

                    SessionInfo info = new SessionInfo(id, client);
                    info.nickname = "player-" + id;
                    synchronized (sessions) {
                        sessions.add(info);
                        sessions.notifyAll();
                    }

                    System.out.println(info.nickname + " dołączono!");

                } catch (SocketException se) {
                    System.out.println("No longer accepting connections: " + se.getMessage());
                    break;
                }
            }

            System.out.println("Reached max players or stopped accepting. Waiting for host to start games...");

            // Wait until host starts
            synchronized (sessions) {
                while (handlers.isEmpty() && !serverSocket.isClosed()) {
                    try {
                        sessions.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            for (Thread t : handlers) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Server interrupted while waiting for players to finish.");
                }
            }

            System.out.println("All player sessions ended. Server shutting down.");
        } finally {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            if (hostThread != null && hostThread.isAlive()) {
                hostThread.interrupt();
            }
        }
    }

    // Made public so external controllers (GUI/CLI) can access session info
    public static class SessionInfo {
        public final int id;
        public final Socket socket;
        public volatile Integer secret;
        public volatile boolean finished = false;
        public volatile boolean started = false;
        public volatile String nickname;

        public volatile int guesses = 0;
        public volatile boolean guessedCorrect = false;
        public volatile PrintWriter out = null;

        public SessionInfo(int id, Socket socket) {
            this.id = id;
            this.socket = socket;
        }

        public void closeSocket() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final SessionInfo info;
        private final int low;
        private final int high;
        private final List<SessionInfo> sessions;

        ClientHandler(SessionInfo info, int low, int high, List<SessionInfo> sessions) {
            this.info = info;
            this.low = low;
            this.high = high;
            this.sessions = sessions;
        }

        @Override
        public void run() {
            Socket client = info.socket;

            try (Socket c = client;
                 BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
                 PrintWriter out = new PrintWriter(c.getOutputStream(), true)) {

                // store writer to send ranking later
                info.out = out;

                int secret = new Random().nextInt(high - low + 1) + low;
                info.secret = secret;

                out.println("START:" + low + ":" + high);

                String firstLine = null;
                try {
                    c.setSoTimeout(5000);
                    firstLine = in.readLine();
                } catch (SocketTimeoutException ignored) {
                } finally {
                    try { c.setSoTimeout(0); } catch (SocketException ignored) {}
                }

                if (firstLine != null && firstLine.startsWith("NICK:")) {
                    String name = firstLine.substring(5).trim();
                    info.nickname = name.isEmpty() ? info.nickname : name;
                    firstLine = null;
                }

                // process guesses until this player guesses correctly or quits
                String line = firstLine;
                boolean localDone = false;
                while (!localDone && (line != null || (line = in.readLine()) != null)) {
                    if (line.startsWith("GUESS:")) {
                        int g;
                        try {
                            g = Integer.parseInt(line.substring(6));
                        } catch (NumberFormatException e) {
                            out.println("RESULT:ERROR");
                            line = null;
                            continue;
                        }

                        info.guesses++;

                        if (g < secret) out.println("RESULT:LOW");
                        else if (g > secret) out.println("RESULT:HIGH");
                        else {
                            out.println("RESULT:CORRECT");
                            // Inform client to stop guessing and wait for others
                            out.println("WAIT");

                            info.guessedCorrect = true;
                            synchronized (sessions) {
                                info.finished = true;
                                sessions.notifyAll();
                            }
                            System.out.println("Client " + c.getRemoteSocketAddress() + " guessed correctly (" + secret + "). Waiting for others.");
                            localDone = true;
                            break;
                        }
                    } else if (line.equals("QUIT")) {
                        out.println("BYE");
                        synchronized (sessions) {
                            info.finished = true;
                            sessions.notifyAll();
                        }
                        localDone = true;
                        break;
                    } else {
                        out.println("RESULT:ERROR");
                    }
                    line = null;
                }

                // Wait until all started players have finished
                synchronized (sessions) {
                    while (true) {
                        boolean allFinished = true;
                        for (SessionInfo s : sessions) {
                            if (s.started && !s.finished) {
                                allFinished = false;
                                break;
                            }
                        }
                        if (allFinished) break;
                        try {
                            sessions.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }

                    if (rankingSent.compareAndSet(false, true)) {
                        // Build ranking for started players who participated
                        List<SessionInfo> ranking = new ArrayList<>();
                        for (SessionInfo s : sessions) {
                            if (s.started) ranking.add(s);
                        }
                        // Sort players who guessed
                        ranking.sort((a, b) -> {
                            boolean aGuessed = a.guessedCorrect;
                            boolean bGuessed = b.guessedCorrect;
                            if (aGuessed && bGuessed) return Integer.compare(a.guesses, b.guesses);
                            if (aGuessed) return -1;
                            if (bGuessed) return 1;
                            return Integer.compare(a.id, b.id);
                        });

                        // Send ranking
                        for (SessionInfo s : sessions) {
                            try {
                                PrintWriter pw = s.out;
                                if (pw == null) {
                                    try {
                                        pw = new PrintWriter(s.socket.getOutputStream(), true);
                                        s.out = pw;
                                    } catch (IOException ioe) {
                                        continue;
                                    }
                                }
                                pw.println("RANKING_START");
                                int pos = 1;
                                for (SessionInfo r : ranking) {
                                    String nick = r.nickname == null ? ("player-" + r.id) : r.nickname;
                                    String result = r.guessedCorrect ? String.valueOf(r.guesses) : "-";
                                    pw.println("RANK:" + pos + ":" + nick + ":" + result);
                                    pos++;
                                }
                                pw.println("RANKING_END");
                                pw.println("BYE");
                                try {
                                    s.socket.close();
                                } catch (IOException ignored) {}
                            } catch (Exception ex) {
                                // ignore per-client send errors
                            }
                        }
                    }
                }

            } catch (IOException e) {
                System.out.println("IO error handling client " + client.getRemoteSocketAddress() + ": " + e.getMessage());
            } finally {
                info.finished = true;
                synchronized (sessions) {
                    sessions.notifyAll();
                }
            }
        }
    }

    private static class HostController implements Runnable {
        private final List<SessionInfo> sessions;
        private final List<Thread> handlers;
        private final ServerSocket serverSocket;
        private final Scanner scanner;
        private final int low;
        private final int high;

        HostController(List<SessionInfo> sessions, List<Thread> handlers, ServerSocket serverSocket, Scanner scanner, int low, int high) {
            this.sessions = sessions;
            this.handlers = handlers;
            this.serverSocket = serverSocket;
            this.scanner = scanner;
            this.low = low;
            this.high = high;
        }

        @Override
        public void run() {
            printHelp();
            while (!Thread.currentThread().isInterrupted()) {
                System.out.print("host> ");
                String line;
                try {
                    if (!scanner.hasNextLine()) break;
                    line = scanner.nextLine().trim();
                } catch (Exception e) {
                    break;
                }
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                String cmd = parts[0].toLowerCase(Locale.ROOT);
                switch (cmd) {
                    case "help" -> printHelp();
                    case "list" -> listSessions();
                    case "reveal" -> {
                        if (parts.length < 2) {
                            System.out.println("Usage: reveal <id>");
                        } else {
                            try {
                                int id = Integer.parseInt(parts[1]);
                                reveal(id);
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid id.");
                            }
                        }
                    }
                    case "kick" -> {
                        if (parts.length < 2) {
                            System.out.println("Usage: kick <id>");
                        } else {
                            try {
                                int id = Integer.parseInt(parts[1]);
                                kick(id);
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid id.");
                            }
                        }
                    }
                    case "start" -> startWaitingSessions();
                    case "quit-server", "shutdown" -> {
                        System.out.println("Shutting down server (no new connections) and disconnecting players...");
                        try {
                            serverSocket.close();
                        } catch (IOException ignored) {
                        }
                        // close all client sockets and notify waiting threads
                        synchronized (sessions) {
                            for (SessionInfo s : sessions) {
                                s.closeSocket();
                            }
                            sessions.notifyAll();
                        }
                        return;
                    }
                    default -> System.out.println("Unknown command. Type help.");
                }
            }
        }

        private void startWaitingSessions() {
            synchronized (sessions) {
                boolean any = false;
                for (SessionInfo s : sessions) {
                    if (!s.started && !s.finished) {
                        ClientHandler handler = new ClientHandler(s, low, high, sessions);
                        Thread t = new Thread(handler, "Player-" + s.id);
                        t.start();
                        handlers.add(t);
                        s.started = true;
                        System.out.println("Started " + (s.nickname == null ? "player-" + s.id : s.nickname));
                        any = true;
                    }
                }
                if (!any) {
                    System.out.println("No waiting players to start.");
                }
                sessions.notifyAll();
            }
        }

        private void printHelp() {
            System.out.println("Host commands: list, reveal <id>, kick <id>, start, quit-server, help");
        }

        private void listSessions() {
            synchronized (sessions) {
                if (sessions.isEmpty()) {
                    System.out.println("No sessions yet.");
                    return;
                }
                for (SessionInfo s : sessions) {
                    System.out.printf("id=%d nick=%s addr=%s secret=%s started=%b finished=%b%n",
                            s.id,
                            s.nickname == null ? "?" : s.nickname,
                            s.socket.getRemoteSocketAddress(),
                            (s.secret == null ? "?" : "assigned"),
                            s.started,
                            s.finished);
                }
            }
        }

        private void reveal(int id) {
            synchronized (sessions) {
                for (SessionInfo s : sessions) {
                    if (s.id == id) {
                        if (s.secret == null) {
                            System.out.println("Secret not assigned yet.");
                        } else {
                            System.out.println("Player " + id + " secret: " + s.secret);
                        }
                        return;
                    }
                }
                System.out.println("No session with id " + id);
            }
        }

        private void kick(int id) {
            synchronized (sessions) {
                for (SessionInfo s : sessions) {
                    if (s.id == id) {
                        System.out.println("Kicking player " + id);
                        s.closeSocket();
                        return;
                    }
                }
                System.out.println("No session with id " + id);
            }
        }
    }

    // for tests
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        int low = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        int high = args.length > 2 ? Integer.parseInt(args[2]) : 100;
        int maxPlayers = args.length > 3 ? Integer.parseInt(args[3]) : 1;
        new NetworkServer(port, low, high, maxPlayers).start();
    }
}