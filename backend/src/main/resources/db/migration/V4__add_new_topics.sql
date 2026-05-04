INSERT INTO topics (name, slug, description, color_hex, is_featured)
VALUES
    ('Java 8',          'java-8',          'Lambdas, streams, Optional, functional interfaces, Date/Time API',  '#E76F00', TRUE),
    ('Java 11',         'java-11',         'var, HttpClient, String methods, modules, Flight Recorder',         '#D4691E', FALSE),
    ('Java 17',         'java-17',         'Sealed classes, records, pattern matching, text blocks',            '#C75D28', TRUE),
    ('Java 21',         'java-21',         'Virtual threads, pattern matching, sequenced collections, scoped values', '#BA5132', TRUE),
    ('Spring Framework','spring',          'Core IoC, AOP, Spring MVC, transaction management',                '#6DB33F', TRUE),
    ('Spring Modules',  'spring-modules',  'Spring Security, Data, Cloud, Batch, Integration, WebFlux',        '#5FA230', TRUE),
    ('NoSQL',           'nosql',           'MongoDB, Redis, Cassandra, DynamoDB, document/key-value/graph stores', '#4FAA4D', FALSE),
    ('Node.js',         'nodejs',          'Event loop, Express, async patterns, npm, middleware, clustering',  '#539E43', TRUE),
    ('JavaScript',      'javascript',      'ES6+, closures, prototypes, promises, DOM, event handling',        '#F0DB4F', TRUE),
    ('C',               'c-programming',   'Pointers, memory management, structs, file I/O, system programming','#5C6BC0', FALSE)
ON CONFLICT (slug) DO NOTHING;
