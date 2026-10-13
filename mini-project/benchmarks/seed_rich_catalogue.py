import subprocess
import requests
import json
import time

BASE_URL = "http://localhost:8080/api"

# 1. Clean junk benchmark books from MongoDB
clean_js = """
db.books.deleteMany({ title: /Concurrency/i });
db.book_copies.deleteMany({ bookId: { $nin: db.books.find({}, {_id: 1}).toArray().map(b => b._id) } });
// Reset available copies for existing valid books
db.books.find().forEach(function(b) {
    var validCopies = db.book_copies.countDocuments({ bookId: b._id, status: "AVAILABLE" });
    if (validCopies === 0) {
        db.book_copies.updateMany({ bookId: b._id }, { $set: { status: "AVAILABLE" } });
        validCopies = db.book_copies.countDocuments({ bookId: b._id });
    }
    db.books.updateOne({ _id: b._id }, { $set: { availableCopies: validCopies, totalCopies: validCopies } });
});
"""

print("[1] Cleaning scrap books and repairing copy balances in MongoDB...")
subprocess.run(["mongosh", "mongodb://127.0.0.1:27017/librarydb", "--eval", clean_js], capture_output=True, text=True)

# 2. Login as Admin
print("[2] Logging in as Admin...")
login_resp = requests.post(f"{BASE_URL}/auth/login", json={"email": "admin@ssn.edu.in", "password": "admin123"})
if login_resp.status_code != 200:
    print(f"Failed to login: {login_resp.status_code} {login_resp.text}")
    exit(1)
token = login_resp.json()["data"]["token"]
headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

# 3. Fetch existing titles
existing_resp = requests.get(f"{BASE_URL}/books?page=0&size=100")
existing_titles = set()
if existing_resp.status_code == 200:
    for b in existing_resp.json().get("data", {}).get("content", []):
        existing_titles.add(b["title"].lower().strip())

print(f"Found {len(existing_titles)} existing unique titles.")

# 4. Rich diverse catalogue
NEW_BOOKS = [
    # Distributed Systems
    {
        "title": "Database Internals: A Deep-Dive into How Distributed Storage Works",
        "isbn": "9781492040347",
        "authors": ["Alex Petrov"],
        "categories": ["Distributed Systems", "Database Design"],
        "publisher": "O'Reilly Media",
        "publicationYear": 2019,
        "description": "A comprehensive guide to internal database architecture, storage engines, distributed consensus algorithms like Raft and Paxos, replication, and concurrency control.",
        "initialCopies": 4
    },
    {
        "title": "Cloud Native Patterns: Designing Change-Tolerant Software",
        "isbn": "9781617294297",
        "authors": ["Cornelia Davis"],
        "categories": ["Distributed Systems", "Cloud Computing", "Software Engineering"],
        "publisher": "Manning Publications",
        "publicationYear": 2019,
        "description": "Essential architectural patterns for resilient cloud platforms, service meshes, circuit breaking, declarative deployments, and scalable microservices.",
        "initialCopies": 3
    },
    {
        "title": "Kafka: The Definitive Guide - Real-Time Data and Stream Processing",
        "isbn": "9781492043089",
        "authors": ["Neha Narkhede", "Gwen Shapira", "Todd Palino"],
        "categories": ["Distributed Systems", "Data Engineering"],
        "publisher": "O'Reilly Media",
        "publicationYear": 2021,
        "description": "Architecting low-latency, event-driven streaming pipelines with Apache Kafka, consumer groups, partition management, and fault-tolerant log replicas.",
        "initialCopies": 4
    },

    # System Design
    {
        "title": "System Design Interview – An Insider's Guide (Volume 1)",
        "isbn": "9798664653403",
        "authors": ["Alex Xu"],
        "categories": ["System Design", "Distributed Systems"],
        "publisher": "ByteByteGo",
        "publicationYear": 2020,
        "description": "Practical step-by-step blueprints for architecting high-scale internet services including rate limiters, key-value stores, web crawlers, chat systems, and notification pipelines.",
        "initialCopies": 5
    },
    {
        "title": "Site Reliability Engineering: How Google Runs Production Systems",
        "isbn": "9781491929124",
        "authors": ["Betsy Beyer", "Chris Jones", "Jennifer Petoff", "Niall Richard Murphy"],
        "categories": ["System Design", "DevOps"],
        "publisher": "O'Reilly Media",
        "publicationYear": 2016,
        "description": "The definitive Google operational playbook covering SLIs, SLOs, error budgets, automated incident response, distributed tracing, and production resilience.",
        "initialCopies": 3
    },
    {
        "title": "High Performance Browser Networking",
        "isbn": "9781449344764",
        "authors": ["Ilya Grigorik"],
        "categories": ["System Design", "Networking", "Computer Science"],
        "publisher": "O'Reilly Media",
        "publicationYear": 2013,
        "description": "In-depth guide to modern network performance: TCP handshake dynamics, TLS 1.3 optimization, HTTP/2 multiplexing, WebSocket channels, and WebRTC streaming.",
        "initialCopies": 3
    },

    # Software Engineering
    {
        "title": "The Pragmatic Programmer: Your Journey To Mastery (20th Anniversary Edition)",
        "isbn": "9780135957059",
        "authors": ["David Thomas", "Andrew Hunt"],
        "categories": ["Software Engineering", "Programming"],
        "publisher": "Addison-Wesley Professional",
        "publicationYear": 2019,
        "description": "Timeless engineering philosophy covering modularity, orthogonality, defensive coding, DRY principles, career development, and refactoring techniques.",
        "initialCopies": 4
    },
    {
        "title": "Refactoring: Improving the Design of Existing Code",
        "isbn": "9780134757599",
        "authors": ["Martin Fowler", "Kent Beck"],
        "categories": ["Software Engineering", "Programming"],
        "publisher": "Addison-Wesley Professional",
        "publicationYear": 2018,
        "description": "The foundational handbook of code smells, automated unit testing, transformation mechanics, and improving legacy codebase maintainability without breaking behavior.",
        "initialCopies": 3
    },
    {
        "title": "Head First Design Patterns: Building Extensible and Maintainable Software",
        "isbn": "9781492078005",
        "authors": ["Eric Freeman", "Elisabeth Robson"],
        "categories": ["Software Engineering", "Design Patterns"],
        "publisher": "O'Reilly Media",
        "publicationYear": 2020,
        "description": "Visually rich, pedagogically proven exploration of Factory, Strategy, Observer, Decorator, Adapter, and Composite design patterns.",
        "initialCopies": 4
    },

    # Computer Science & Algorithms
    {
        "title": "The Algorithm Design Manual (3rd Edition)",
        "isbn": "9783030542559",
        "authors": ["Steven S. Skiena"],
        "categories": ["Computer Science", "Algorithms"],
        "publisher": "Springer",
        "publicationYear": 2020,
        "description": "A premier reference manual and war-story guide for algorithmic engineering, combinatorial problems, graph algorithms, and asymptotic performance tuning.",
        "initialCopies": 4
    },
    {
        "title": "Compilers: Principles, Techniques, and Tools (The Dragon Book)",
        "isbn": "9780321486813",
        "authors": ["Alfred V. Aho", "Monica S. Lam", "Ravi Sethi", "Jeffrey D. Ullman"],
        "categories": ["Computer Science", "Programming Languages"],
        "publisher": "Pearson",
        "publicationYear": 2006,
        "description": "The classic Dragon Book on lexical analysis, context-free grammars, LL/LR parsing, intermediate representations, register allocation, and code generation.",
        "initialCopies": 3
    },

    # Electrical & Electronics Engineering
    {
        "title": "The Art of Electronics (3rd Edition)",
        "isbn": "9780521809269",
        "authors": ["Paul Horowitz", "Winfield Hill"],
        "categories": ["Electrical Engineering", "Electronics"],
        "publisher": "Cambridge University Press",
        "publicationYear": 2015,
        "description": "Widely accepted as the gold standard in circuit design, analog signal processing, operational amplifiers, voltage regulation, and precision instrumentation.",
        "initialCopies": 4
    },
    {
        "title": "Microelectronic Circuits (8th Edition)",
        "isbn": "9780190853464",
        "authors": ["Adel S. Sedra", "Kenneth C. Smith", "Tony Chan Carusone"],
        "categories": ["Electrical Engineering", "Electronics"],
        "publisher": "Oxford University Press",
        "publicationYear": 2020,
        "description": "The definitive undergraduate foundation in semiconductor physics, MOSFET amplifiers, bipolar junction transistors, IC biasing, and high-frequency response.",
        "initialCopies": 3
    },
    {
        "title": "Digital Design: With an Introduction to the Verilog HDL",
        "isbn": "9780132774208",
        "authors": ["M. Morris Mano", "Michael D. Ciletti"],
        "categories": ["Electrical Engineering", "Computer Engineering"],
        "publisher": "Pearson",
        "publicationYear": 2012,
        "description": "Clear presentation of Boolean algebra, combinational logic design, synchronous sequential circuits, finite state machines, and HDL hardware description.",
        "initialCopies": 4
    },

    # Mechanical & Civil Engineering
    {
        "title": "Shigley's Mechanical Engineering Design (11th Edition)",
        "isbn": "9780073398204",
        "authors": ["Richard G. Budynas", "J. Keith Nisbett"],
        "categories": ["Mechanical Engineering"],
        "publisher": "McGraw-Hill Education",
        "publicationYear": 2019,
        "description": "The standard reference in machine element design, stress analysis, fatigue failure theories, lubrication, shafts, gears, bearings, and fasteners.",
        "initialCopies": 4
    },
    {
        "title": "Fluid Mechanics (9th Edition)",
        "isbn": "9781260113211",
        "authors": ["Frank M. White", "Henry Xue"],
        "categories": ["Mechanical Engineering", "Civil Engineering"],
        "publisher": "McGraw-Hill Education",
        "publicationYear": 2021,
        "description": "Comprehensive treatment of control volume mechanics, Navier-Stokes equations, boundary layer theory, pipe flow, and compressible aerodynamics.",
        "initialCopies": 3
    },
    {
        "title": "Structural Analysis (10th Edition)",
        "isbn": "9780134610672",
        "authors": ["Russell C. Hibbeler"],
        "categories": ["Civil Engineering"],
        "publisher": "Pearson",
        "publicationYear": 2017,
        "description": "Classical and matrix methods for structural equilibrium, trusses, beam deflection, moment distribution, and finite element modeling of civil infrastructures.",
        "initialCopies": 3
    },

    # Mathematics & Data Science
    {
        "title": "Pattern Recognition and Machine Learning",
        "isbn": "9780387310732",
        "authors": ["Christopher M. Bishop"],
        "categories": ["Mathematics", "Data Science", "Machine Learning"],
        "publisher": "Springer",
        "publicationYear": 2006,
        "description": "Bayesian probability theory, Gaussian mixture models, expectation-maximization, support vector machines, and kernel methods for machine intelligence.",
        "initialCopies": 4
    },
    {
        "title": "Deep Learning (Adaptive Computation and Machine Learning)",
        "isbn": "9780262035612",
        "authors": ["Ian Goodfellow", "Yoshua Bengio", "Aaron Courville"],
        "categories": ["Data Science", "Artificial Intelligence"],
        "publisher": "MIT Press",
        "publicationYear": 2016,
        "description": "Theoretical and mathematical foundations of deep feedforward networks, convolution, recurrent architectures, autoencoders, and generative modeling.",
        "initialCopies": 4
    },
    {
        "title": "Linear Algebra and Its Applications (5th Edition)",
        "isbn": "9780321982384",
        "authors": ["Gilbert Strang"],
        "categories": ["Mathematics"],
        "publisher": "Cengage Learning",
        "publicationYear": 2016,
        "description": "Vector spaces, linear transformations, orthogonality, eigenvalues, eigenvectors, and singular value decomposition (SVD) with engineering applications.",
        "initialCopies": 5
    },

    # Humanities & General Reading
    {
        "title": "Sapiens: A Brief History of Humankind",
        "isbn": "9780062316097",
        "authors": ["Yuval Noah Harari"],
        "categories": ["Humanities", "History", "General Reading"],
        "publisher": "Harper",
        "publicationYear": 2015,
        "description": "A sweeping narrative exploring the Cognitive, Agricultural, and Scientific Revolutions that shaped human civilization, culture, and social constructs.",
        "initialCopies": 5
    },
    {
        "title": "Thinking, Fast and Slow",
        "isbn": "9780374533557",
        "authors": ["Daniel Kahneman"],
        "categories": ["Humanities", "Psychology", "General Reading"],
        "publisher": "Farrar, Straus and Giroux",
        "publicationYear": 2011,
        "description": "Nobel Laureate Daniel Kahneman's seminal exploration of dual-process cognitive psychology: System 1 (intuitive/fast) versus System 2 (deliberative/slow).",
        "initialCopies": 4
    },
    {
        "title": "The Design of Everyday Things",
        "isbn": "9780465050659",
        "authors": ["Don Norman"],
        "categories": ["Design", "Humanities", "General Reading"],
        "publisher": "Basic Books",
        "publicationYear": 2013,
        "description": "The quintessential guide to human-centered design, cognitive affordances, signifiers, conceptual models, and ergonomic feedback loops.",
        "initialCopies": 4
    },
    {
        "title": "Gödel, Escher, Bach: An Eternal Golden Braid",
        "isbn": "9780465026562",
        "authors": ["Douglas Hofstadter"],
        "categories": ["Philosophy", "Mathematics", "Computer Science"],
        "publisher": "Basic Books",
        "publicationYear": 1999,
        "description": "Pulitzer Prize-winning exploration of recursion, strange loops, self-reference, formal mathematical logic, and the mechanics of human cognition.",
        "initialCopies": 3
    }
]

added_count = 0
for b in NEW_BOOKS:
    if b["title"].lower().strip() in existing_titles:
        print(f" -> Already exists: {b['title']}")
        continue
    resp = requests.post(f"{BASE_URL}/books", headers=headers, json=b)
    if resp.status_code == 201:
        added_count += 1
        print(f" -> Added ({b['categories'][0]}): {b['title']} (Copies: {b['initialCopies']})")
    else:
        print(f" -> Failed to add {b['title']}: {resp.status_code} {resp.text}")

print(f"\n[DONE] Successfully seeded {added_count} new academic titles across all domains!")
