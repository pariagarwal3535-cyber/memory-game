package quiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores all quiz questions for all subjects.
 * Questions are built in code - no external files needed.
 * Each subject has 30+ questions across Easy/Medium/Hard.
 */
public class QuizBank {

    private static final List<Question> ALL_QUESTIONS = new ArrayList<Question>();

    static {
        loadGK();
        loadEnglish();
        loadOS();
        loadDSA();
        loadNetworks();
        loadDBMS();
        loadOOP();
        loadAlgorithms();
    }

    // ---- GK Questions ----
    private static void loadGK() {
        Question.Subject s = Question.Subject.GK;
        add(1, "What is the capital of India?",
            new String[]{"Mumbai", "New Delhi", "Kolkata", "Chennai"}, 1,
            "New Delhi has been the capital of India since 1911.", s, Question.Difficulty.EASY);
        add(2, "Which is the largest planet in our solar system?",
            new String[]{"Saturn", "Neptune", "Jupiter", "Uranus"}, 2,
            "Jupiter is the largest planet, 11 times wider than Earth.", s, Question.Difficulty.EASY);
        add(3, "How many continents are there on Earth?",
            new String[]{"5", "6", "7", "8"}, 2,
            "Earth has 7 continents: Asia, Africa, North America, South America, Antarctica, Europe, Australia.", s, Question.Difficulty.EASY);
        add(4, "Which country invented the game of Chess?",
            new String[]{"China", "Greece", "India", "Persia"}, 2,
            "Chess originated in India during the Gupta Empire around the 6th century AD.", s, Question.Difficulty.MEDIUM);
        add(5, "What is the chemical symbol for Gold?",
            new String[]{"Go", "Gd", "Au", "Ag"}, 2,
            "Au comes from the Latin word Aurum meaning gold.", s, Question.Difficulty.EASY);
        add(6, "Which is the longest river in the world?",
            new String[]{"Amazon", "Nile", "Yangtze", "Mississippi"}, 1,
            "The Nile River in Africa is approximately 6,650 km long.", s, Question.Difficulty.EASY);
        add(7, "Who wrote the Indian National Anthem?",
            new String[]{"Bankim Chandra", "Rabindranath Tagore", "Sarojini Naidu", "Mahatma Gandhi"}, 1,
            "Jana Gana Mana was written by Rabindranath Tagore in 1911.", s, Question.Difficulty.EASY);
        add(8, "What is the smallest country in the world?",
            new String[]{"Monaco", "San Marino", "Vatican City", "Liechtenstein"}, 2,
            "Vatican City covers just 0.44 square kilometers.", s, Question.Difficulty.MEDIUM);
        add(9, "Which planet is known as the Red Planet?",
            new String[]{"Venus", "Mars", "Jupiter", "Mercury"}, 1,
            "Mars appears red due to iron oxide (rust) on its surface.", s, Question.Difficulty.EASY);
        add(10, "How many bones are in the adult human body?",
            new String[]{"196", "206", "216", "226"}, 1,
            "Adults have 206 bones. Babies are born with around 270-300 bones that fuse over time.", s, Question.Difficulty.MEDIUM);
        add(11, "Which gas do plants absorb from the atmosphere?",
            new String[]{"Oxygen", "Nitrogen", "Carbon Dioxide", "Hydrogen"}, 2,
            "Plants absorb CO2 during photosynthesis and release oxygen.", s, Question.Difficulty.EASY);
        add(12, "Who was the first person to walk on the Moon?",
            new String[]{"Buzz Aldrin", "Yuri Gagarin", "Neil Armstrong", "John Glenn"}, 2,
            "Neil Armstrong walked on the Moon on July 20, 1969 during Apollo 11.", s, Question.Difficulty.EASY);
        add(13, "What is the currency of Japan?",
            new String[]{"Yuan", "Won", "Yen", "Rupee"}, 2,
            "The Japanese Yen (JPY) is the official currency of Japan.", s, Question.Difficulty.EASY);
        add(14, "Which ocean is the largest?",
            new String[]{"Atlantic", "Indian", "Arctic", "Pacific"}, 3,
            "The Pacific Ocean covers more than 30% of Earth's surface.", s, Question.Difficulty.EASY);
        add(15, "What is the hardest natural substance on Earth?",
            new String[]{"Gold", "Iron", "Diamond", "Platinum"}, 2,
            "Diamond rates 10 on the Mohs hardness scale - the maximum.", s, Question.Difficulty.EASY);
    }

    // ---- English Grammar Questions ----
    private static void loadEnglish() {
        Question.Subject s = Question.Subject.ENGLISH;
        add(101, "Choose the correct sentence:",
            new String[]{"She don't like tea.", "She doesn't likes tea.", "She doesn't like tea.", "She not like tea."}, 2,
            "With third person singular (she/he/it), use 'doesn't' + base verb.", s, Question.Difficulty.EASY);
        add(102, "What is the plural of 'child'?",
            new String[]{"Childs", "Childes", "Children", "Childrens"}, 2,
            "Child has an irregular plural: children.", s, Question.Difficulty.EASY);
        add(103, "Which word is a synonym of 'Happy'?",
            new String[]{"Sad", "Joyful", "Angry", "Tired"}, 1,
            "Joyful means feeling great happiness - a synonym of happy.", s, Question.Difficulty.EASY);
        add(104, "Fill in: 'I have been working here ___ 5 years.'",
            new String[]{"since", "for", "from", "during"}, 1,
            "Use 'for' with a duration of time (5 years). Use 'since' with a point in time.", s, Question.Difficulty.MEDIUM);
        add(105, "What part of speech is the word 'quickly'?",
            new String[]{"Adjective", "Noun", "Adverb", "Verb"}, 2,
            "Quickly modifies a verb (run quickly) so it is an adverb.", s, Question.Difficulty.EASY);
        add(106, "Choose the correct word: 'Their / There / They're going home.'",
            new String[]{"Their", "There", "They're", "Thier"}, 2,
            "They're = They are. Their = possessive. There = place.", s, Question.Difficulty.MEDIUM);
        add(107, "What is the antonym of 'Ancient'?",
            new String[]{"Old", "Historic", "Modern", "Classic"}, 2,
            "Modern means current/new, the opposite of ancient.", s, Question.Difficulty.EASY);
        add(108, "Which sentence uses the Past Perfect tense?",
            new String[]{"She was eating.", "She had eaten.", "She has eaten.", "She ate."}, 1,
            "Past Perfect = had + past participle (had eaten).", s, Question.Difficulty.MEDIUM);
        add(109, "Identify the noun in: 'The cat sat on the mat.'",
            new String[]{"sat", "on", "cat", "the"}, 2,
            "Cat is a noun - it names a thing/animal.", s, Question.Difficulty.EASY);
        add(110, "What does 'Benevolent' mean?",
            new String[]{"Cruel", "Lazy", "Kind and generous", "Angry"}, 2,
            "Benevolent means well-meaning and kind - from Latin bene (well) + volent (wish).", s, Question.Difficulty.HARD);
        add(111, "Fill in: 'Neither the boys nor the girl ___ ready.'",
            new String[]{"are", "is", "were", "have"}, 1,
            "When using Neither/Nor, the verb agrees with the nearest subject (girl = singular = is).", s, Question.Difficulty.HARD);
        add(112, "Which is the correct spelling?",
            new String[]{"Accomodation", "Accommodation", "Acommodation", "Accommodaton"}, 1,
            "Accommodation has double c and double m.", s, Question.Difficulty.MEDIUM);
    }

    // ---- Operating Systems Questions ----
    private static void loadOS() {
        Question.Subject s = Question.Subject.OPERATING_SYSTEMS;
        add(201, "Which scheduling algorithm gives the minimum average waiting time?",
            new String[]{"FCFS", "Round Robin", "SJF", "Priority"}, 2,
            "Shortest Job First (SJF) minimizes average waiting time among non-preemptive algorithms.", s, Question.Difficulty.MEDIUM);
        add(202, "What does OS stand for?",
            new String[]{"Open System", "Operating System", "Output System", "Optimal System"}, 1,
            "OS stands for Operating System - software that manages computer hardware.", s, Question.Difficulty.EASY);
        add(203, "Which of these is NOT an operating system?",
            new String[]{"Linux", "Windows", "Oracle", "macOS"}, 2,
            "Oracle is a database management system, not an OS.", s, Question.Difficulty.EASY);
        add(204, "What is a deadlock?",
            new String[]{"A fast process", "A situation where processes wait forever for resources", "Memory overflow", "CPU crash"}, 1,
            "Deadlock occurs when processes are stuck waiting for each other's resources indefinitely.", s, Question.Difficulty.MEDIUM);
        add(205, "What does FCFS stand for?",
            new String[]{"First Come First Served", "Fast CPU First Scheduling", "First CPU First Served", "Fixed CPU First Schedule"}, 0,
            "FCFS processes jobs in the order they arrive - simple but not optimal.", s, Question.Difficulty.EASY);
        add(206, "Virtual memory allows programs to use more memory than:",
            new String[]{"ROM size", "Cache size", "Physical RAM size", "Hard disk size"}, 2,
            "Virtual memory uses disk space to extend available RAM for running programs.", s, Question.Difficulty.MEDIUM);
        add(207, "Which condition is NOT required for deadlock?",
            new String[]{"Mutual Exclusion", "Hold and Wait", "Preemption", "Circular Wait"}, 2,
            "The 4 deadlock conditions are: Mutual Exclusion, Hold and Wait, No Preemption, Circular Wait.", s, Question.Difficulty.HARD);
        add(208, "What is thrashing in OS?",
            new String[]{"Fast page swapping", "Excessive paging causing low CPU utilization", "Memory allocation", "Process scheduling"}, 1,
            "Thrashing occurs when the system spends more time swapping pages than executing processes.", s, Question.Difficulty.HARD);
        add(209, "Which memory management technique eliminates external fragmentation?",
            new String[]{"Paging", "Segmentation", "Partitioning", "Swapping"}, 0,
            "Paging divides memory into fixed-size frames, eliminating external fragmentation.", s, Question.Difficulty.MEDIUM);
        add(210, "What is the purpose of a semaphore?",
            new String[]{"Memory allocation", "Process synchronization", "CPU scheduling", "File management"}, 1,
            "Semaphores are used to control access to shared resources and synchronize processes.", s, Question.Difficulty.MEDIUM);
        add(211, "Round Robin scheduling uses:",
            new String[]{"Priority queues", "Time quantum", "Job length", "Memory size"}, 1,
            "Round Robin gives each process a fixed time quantum in circular order.", s, Question.Difficulty.EASY);
        add(212, "What is a process in OS?",
            new String[]{"A program on disk", "A program in execution", "A file in memory", "A CPU instruction"}, 1,
            "A process is a program that is currently being executed by the CPU.", s, Question.Difficulty.EASY);
    }

    // ---- Data Structures Questions ----
    private static void loadDSA() {
        Question.Subject s = Question.Subject.DATA_STRUCTURES;
        add(301, "Which data structure uses LIFO (Last In First Out)?",
            new String[]{"Queue", "Stack", "Array", "Tree"}, 1,
            "Stack follows LIFO - the last element pushed is the first to be popped.", s, Question.Difficulty.EASY);
        add(302, "What is the time complexity of Binary Search?",
            new String[]{"O(n)", "O(n2)", "O(log n)", "O(1)"}, 2,
            "Binary Search halves the search space each step, giving O(log n) complexity.", s, Question.Difficulty.MEDIUM);
        add(303, "Which data structure uses FIFO (First In First Out)?",
            new String[]{"Stack", "Tree", "Queue", "Graph"}, 2,
            "Queue follows FIFO - elements are added at rear and removed from front.", s, Question.Difficulty.EASY);
        add(304, "What is the worst case time complexity of Bubble Sort?",
            new String[]{"O(n)", "O(log n)", "O(n log n)", "O(n2)"}, 3,
            "Bubble Sort compares adjacent elements repeatedly, giving O(n2) in worst case.", s, Question.Difficulty.MEDIUM);
        add(305, "In a Binary Search Tree, values in the left subtree are:",
            new String[]{"Greater than root", "Less than root", "Equal to root", "Random"}, 1,
            "BST property: left subtree < root < right subtree.", s, Question.Difficulty.EASY);
        add(306, "What is a linked list?",
            new String[]{"Array of fixed size", "Sequence of nodes with pointers", "Tree structure", "Hash table"}, 1,
            "A linked list consists of nodes where each node contains data and a pointer to the next node.", s, Question.Difficulty.EASY);
        add(307, "Which sorting algorithm has best average case complexity?",
            new String[]{"Bubble Sort", "Selection Sort", "Merge Sort", "Insertion Sort"}, 2,
            "Merge Sort has O(n log n) average case, which is optimal for comparison-based sorting.", s, Question.Difficulty.MEDIUM);
        add(308, "What is the maximum number of nodes in a binary tree of height h?",
            new String[]{"2h", "2h+1 - 1", "h2", "2h - 1"}, 1,
            "A full binary tree of height h has maximum 2^(h+1) - 1 nodes.", s, Question.Difficulty.HARD);
        add(309, "Which data structure is used for BFS traversal?",
            new String[]{"Stack", "Queue", "Tree", "Array"}, 1,
            "BFS (Breadth First Search) uses a queue to visit nodes level by level.", s, Question.Difficulty.MEDIUM);
        add(310, "What is a Hash Table used for?",
            new String[]{"Sorting data", "Fast key-value lookup", "Tree traversal", "Graph search"}, 1,
            "Hash tables provide O(1) average time for insert, delete, and search operations.", s, Question.Difficulty.EASY);
        add(311, "Which traversal visits root FIRST in a binary tree?",
            new String[]{"Inorder", "Postorder", "Preorder", "Level order"}, 2,
            "Preorder traversal: Root, Left, Right. Inorder: Left, Root, Right. Postorder: Left, Right, Root.", s, Question.Difficulty.MEDIUM);
        add(312, "What is the space complexity of Merge Sort?",
            new String[]{"O(1)", "O(log n)", "O(n)", "O(n2)"}, 2,
            "Merge Sort requires O(n) extra space for the temporary arrays during merging.", s, Question.Difficulty.HARD);
    }

    // ---- Computer Networks Questions ----
    private static void loadNetworks() {
        Question.Subject s = Question.Subject.COMPUTER_NETWORKS;
        add(401, "Which layer of OSI model handles routing?",
            new String[]{"Physical", "Data Link", "Network", "Transport"}, 2,
            "The Network Layer (Layer 3) handles logical addressing and routing of packets.", s, Question.Difficulty.MEDIUM);
        add(402, "What does HTTP stand for?",
            new String[]{"HyperText Transfer Protocol", "High Transfer Text Protocol", "Hyperlink Text Protocol", "Host Transfer Protocol"}, 0,
            "HTTP is the foundation of data communication on the World Wide Web.", s, Question.Difficulty.EASY);
        add(403, "Which protocol is used for sending emails?",
            new String[]{"HTTP", "FTP", "SMTP", "DNS"}, 2,
            "SMTP (Simple Mail Transfer Protocol) is used for sending emails between servers.", s, Question.Difficulty.EASY);
        add(404, "What is the purpose of DNS?",
            new String[]{"Transfer files", "Translate domain names to IP addresses", "Encrypt data", "Route packets"}, 1,
            "DNS (Domain Name System) converts human-readable domain names to IP addresses.", s, Question.Difficulty.EASY);
        add(405, "Which is a private IP address?",
            new String[]{"8.8.8.8", "192.168.1.1", "216.58.214.164", "142.250.80.46"}, 1,
            "192.168.x.x is a private IP range used in local networks.", s, Question.Difficulty.MEDIUM);
        add(406, "TCP is different from UDP because TCP is:",
            new String[]{"Faster", "Connection-less", "Connection-oriented", "Unreliable"}, 2,
            "TCP establishes a connection before sending data and guarantees delivery.", s, Question.Difficulty.MEDIUM);
        add(407, "What does IP stand for?",
            new String[]{"Internet Program", "Internal Protocol", "Internet Protocol", "Input Protocol"}, 2,
            "IP (Internet Protocol) is responsible for addressing and routing packets.", s, Question.Difficulty.EASY);
        add(408, "How many layers does the OSI model have?",
            new String[]{"4", "5", "6", "7"}, 3,
            "OSI model has 7 layers: Physical, Data Link, Network, Transport, Session, Presentation, Application.", s, Question.Difficulty.EASY);
        add(409, "Which device connects different networks together?",
            new String[]{"Hub", "Switch", "Router", "Repeater"}, 2,
            "A router connects different networks and directs traffic between them using IP addresses.", s, Question.Difficulty.EASY);
        add(410, "What is the default port for HTTPS?",
            new String[]{"80", "21", "443", "22"}, 2,
            "HTTPS uses port 443. HTTP uses port 80. FTP uses 21. SSH uses 22.", s, Question.Difficulty.MEDIUM);
    }

    // ---- DBMS Questions ----
    private static void loadDBMS() {
        Question.Subject s = Question.Subject.DBMS;
        add(501, "What does DBMS stand for?",
            new String[]{"Data Based Management System", "Database Management System", "Data Base Manage System", "Digital Base Management"}, 1,
            "DBMS is software that manages databases and provides controlled access to data.", s, Question.Difficulty.EASY);
        add(502, "What does ACID stand for in DBMS?",
            new String[]{"Atomicity, Consistency, Isolation, Durability", "Access, Control, Index, Data", "Atomic, Correct, Isolated, Durable", "Array, Class, Index, Database"}, 0,
            "ACID properties ensure reliable database transactions.", s, Question.Difficulty.MEDIUM);
        add(503, "Which key uniquely identifies each row in a table?",
            new String[]{"Foreign Key", "Candidate Key", "Primary Key", "Super Key"}, 2,
            "A Primary Key uniquely identifies each record in a database table.", s, Question.Difficulty.EASY);
        add(504, "What is a Foreign Key?",
            new String[]{"Key that identifies a table", "Key linking two tables", "Key with no duplicates", "Key with NULL values"}, 1,
            "A Foreign Key in one table references the Primary Key of another table, creating a link.", s, Question.Difficulty.EASY);
        add(505, "What does SQL stand for?",
            new String[]{"Structured Query Language", "Simple Query Language", "Standard Query Logic", "System Query Language"}, 0,
            "SQL is used to manage and query relational databases.", s, Question.Difficulty.EASY);
        add(506, "Which SQL command retrieves data from a table?",
            new String[]{"INSERT", "UPDATE", "SELECT", "DELETE"}, 2,
            "SELECT is used to query and retrieve data from database tables.", s, Question.Difficulty.EASY);
        add(507, "What is normalization in DBMS?",
            new String[]{"Encrypting data", "Organizing data to reduce redundancy", "Deleting duplicate records", "Indexing tables"}, 1,
            "Normalization organizes tables to minimize data redundancy and improve integrity.", s, Question.Difficulty.MEDIUM);
        add(508, "Which join returns all rows from both tables?",
            new String[]{"INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL OUTER JOIN"}, 3,
            "FULL OUTER JOIN returns all rows from both tables, with NULLs where no match exists.", s, Question.Difficulty.MEDIUM);
        add(509, "What is an index in DBMS?",
            new String[]{"A type of key", "A data structure for faster search", "A backup of data", "A type of query"}, 1,
            "An index is a data structure that improves the speed of data retrieval operations.", s, Question.Difficulty.MEDIUM);
        add(510, "Which normal form removes partial dependencies?",
            new String[]{"1NF", "2NF", "3NF", "BCNF"}, 1,
            "2NF removes partial dependencies - every non-key attribute must depend on the WHOLE primary key.", s, Question.Difficulty.HARD);
    }

    // ---- OOP Questions ----
    private static void loadOOP() {
        Question.Subject s = Question.Subject.OOP;
        add(601, "What is Encapsulation in OOP?",
            new String[]{"Hiding internal details", "Creating multiple objects", "Inheriting properties", "Overriding methods"}, 0,
            "Encapsulation bundles data and methods together and restricts direct access to internals.", s, Question.Difficulty.EASY);
        add(602, "What is Inheritance in OOP?",
            new String[]{"Creating new classes from scratch", "A class acquiring properties of another class", "Hiding data", "Multiple methods with same name"}, 1,
            "Inheritance allows a child class to inherit properties and methods from a parent class.", s, Question.Difficulty.EASY);
        add(603, "What is Polymorphism?",
            new String[]{"One class, one form", "One name, many forms", "Many classes, one object", "One object, one method"}, 1,
            "Polymorphism allows the same method name to behave differently based on the object.", s, Question.Difficulty.EASY);
        add(604, "What is an Abstract class?",
            new String[]{"A class with all methods implemented", "A class that cannot be instantiated", "A class with no methods", "A final class"}, 1,
            "Abstract classes cannot be instantiated directly - they serve as blueprints for subclasses.", s, Question.Difficulty.MEDIUM);
        add(605, "What is method overloading?",
            new String[]{"Same method name, different parameters", "Same method name, same parameters", "Overriding parent method", "Hiding parent method"}, 0,
            "Method overloading allows multiple methods with the same name but different parameters.", s, Question.Difficulty.EASY);
        add(606, "What keyword is used to inherit a class in Java?",
            new String[]{"implements", "inherits", "extends", "super"}, 2,
            "In Java, 'extends' keyword is used for class inheritance.", s, Question.Difficulty.EASY);
        add(607, "What is an Interface in Java?",
            new String[]{"A class with constructors", "A blueprint with abstract methods only", "A static class", "A final class"}, 1,
            "An interface defines a contract - it contains abstract methods that implementing classes must define.", s, Question.Difficulty.MEDIUM);
        add(608, "What does the 'super' keyword do in Java?",
            new String[]{"Creates new object", "Refers to parent class", "Makes method static", "Hides method"}, 1,
            "The 'super' keyword refers to the parent class and can call parent constructors/methods.", s, Question.Difficulty.MEDIUM);
    }

    // ---- Algorithms Questions ----
    private static void loadAlgorithms() {
        Question.Subject s = Question.Subject.ALGORITHMS;
        add(701, "What is the time complexity of Linear Search?",
            new String[]{"O(1)", "O(log n)", "O(n)", "O(n2)"}, 2,
            "Linear Search checks each element one by one, giving O(n) time complexity.", s, Question.Difficulty.EASY);
        add(702, "Which algorithm uses Divide and Conquer?",
            new String[]{"Bubble Sort", "Insertion Sort", "Merge Sort", "Selection Sort"}, 2,
            "Merge Sort divides the array in half recursively then merges sorted halves.", s, Question.Difficulty.MEDIUM);
        add(703, "What is the best case complexity of Quick Sort?",
            new String[]{"O(n2)", "O(n log n)", "O(n)", "O(log n)"}, 1,
            "Quick Sort best case is O(n log n) when pivot always divides array into equal halves.", s, Question.Difficulty.MEDIUM);
        add(704, "What is Dynamic Programming?",
            new String[]{"Programming with dynamic variables", "Solving problems by storing subproblem results", "A programming language", "Runtime code generation"}, 1,
            "Dynamic Programming breaks problems into overlapping subproblems and stores results to avoid recomputation.", s, Question.Difficulty.MEDIUM);
        add(705, "Which algorithm finds shortest path in a weighted graph?",
            new String[]{"BFS", "DFS", "Dijkstra", "Kruskal"}, 2,
            "Dijkstra's algorithm finds the shortest path from a source to all other vertices.", s, Question.Difficulty.MEDIUM);
        add(706, "What is the time complexity of accessing an element in an array?",
            new String[]{"O(n)", "O(log n)", "O(1)", "O(n2)"}, 2,
            "Array elements can be accessed directly by index, giving O(1) constant time.", s, Question.Difficulty.EASY);
        add(707, "Greedy algorithms make choices that are:",
            new String[]{"Globally optimal always", "Locally optimal at each step", "Random", "Based on future choices"}, 1,
            "Greedy algorithms pick the locally optimal choice at each step, hoping for global optimum.", s, Question.Difficulty.MEDIUM);
        add(708, "What is the purpose of Kruskal's algorithm?",
            new String[]{"Shortest path", "Minimum Spanning Tree", "Graph coloring", "Topological sort"}, 1,
            "Kruskal's algorithm finds the Minimum Spanning Tree of a weighted graph.", s, Question.Difficulty.HARD);
    }

    // ---- Helper ----
    private static void add(int id, String q, String[] opts, int correct,
                             String exp, Question.Subject subj, Question.Difficulty diff) {
        ALL_QUESTIONS.add(new Question(id, q, opts, correct, exp, subj, diff));
    }

    // ---- Public API ----

    /** Get all questions for a subject */
    public static List<Question> getBySubject(Question.Subject subject) {
        List<Question> result = new ArrayList<Question>();
        for (Question q : ALL_QUESTIONS) {
            if (q.getSubject() == subject) result.add(q);
        }
        return result;
    }

    /** Get questions filtered by subject and difficulty */
    public static List<Question> getBySubjectAndDifficulty(
            Question.Subject subject, Question.Difficulty difficulty) {
        List<Question> result = new ArrayList<Question>();
        for (Question q : ALL_QUESTIONS) {
            if (q.getSubject() == subject && q.getDifficulty() == difficulty)
                result.add(q);
        }
        return result;
    }

    /** Get N random questions for a subject, shuffled */
    public static List<Question> getRandom(Question.Subject subject, int count) {
        List<Question> all = getBySubject(subject);
        Collections.shuffle(all);
        return all.subList(0, Math.min(count, all.size()));
    }

    /** Get all subjects available */
    public static Question.Subject[] getAllSubjects() {
        return Question.Subject.values();
    }

    public static int getTotalCount() { return ALL_QUESTIONS.size(); }
}