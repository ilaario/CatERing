-- database: catering.db
-- 1) FIRST REMOVE ALL TABLES (in reverse dependency order)
DROP TABLE IF EXISTS `Assignment`;

DROP TABLE IF EXISTS `Tasks`;

DROP TABLE IF EXISTS `SummarySheets`;

DROP TABLE IF EXISTS `ShiftBookings`;

DROP TABLE IF EXISTS `Shifts`;

DROP TABLE IF EXISTS `RecipePreparations`;

DROP TABLE IF EXISTS `UserRoles`;

DROP TABLE IF EXISTS `Services`;

DROP TABLE IF EXISTS `MenuItems`;

DROP TABLE IF EXISTS `MenuFeatures`;

DROP TABLE IF EXISTS `MenuSections`;

DROP TABLE IF EXISTS `Menus`;

DROP TABLE IF EXISTS `Events`;

DROP TABLE IF EXISTS `Preparations`;

DROP TABLE IF EXISTS `Recipes`;

DROP TABLE IF EXISTS `Roles`;

DROP TABLE IF EXISTS `Users`;

-- 2) CREATE ALL TABLES (in dependency order)
-- Start with tables that don't depend on others
CREATE TABLE
    `Users` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT,
        `username` TEXT NOT NULL DEFAULT ''
    );

CREATE TABLE
    `Roles` (
        `id` INTEGER NOT NULL,
        `role` TEXT NOT NULL,
        PRIMARY KEY (`id`)
    );

CREATE TABLE
    `Recipes` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT,
        `name` TEXT,
        `description` TEXT DEFAULT ''
    );

CREATE TABLE
    `Preparations` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT,
        `name` TEXT,
        `description` TEXT DEFAULT ''
    );

-- Tables with simple dependencies
CREATE TABLE
    `Events` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT,
        `name` TEXT,
        `date_start` DATE,
        `date_end` DATE,
        `chef_id` INTEGER NOT NULL
    );

CREATE TABLE
    `Menus` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT,
        `title` TEXT,
        `owner_id` INTEGER,
        `published` INTEGER DEFAULT 0
    );

CREATE TABLE
    `MenuSections` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT,
        `menu_id` INTEGER NOT NULL,
        `name` TEXT,
        `position` INTEGER
    );

CREATE TABLE
    `UserRoles` (
        `user_id` INTEGER NOT NULL,
        `role_id` INT NOT NULL DEFAULT 0
    );

CREATE TABLE
    `RecipePreparations` (
        `recipe_id` INTEGER,
        `preparation_id` INTEGER,
        PRIMARY KEY (`recipe_id`, `preparation_id`),
        FOREIGN KEY (`recipe_id`) REFERENCES `Recipes` (`id`),
        FOREIGN KEY (`preparation_id`) REFERENCES `Preparations` (`id`)
    );

CREATE TABLE
    `MenuFeatures` (
        `menu_id` INTEGER NOT NULL,
        `name` TEXT NOT NULL DEFAULT '',
        `value` INTEGER DEFAULT 0
    );

CREATE TABLE
    `MenuItems` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT,
        `menu_id` INTEGER NOT NULL,
        `section_id` INTEGER,
        `description` TEXT,
        `recipe_id` INTEGER NOT NULL,
        `position` INTEGER
    );

CREATE TABLE
    `Services` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT,
        `event_id` INTEGER NOT NULL,
        `name` TEXT,
        `approved_menu_id` INTEGER DEFAULT 0,
        `service_date` DATE,
        `time_start` TIME,
        `time_end` TIME,
        `location` TEXT
    );

CREATE TABLE
    `Shifts` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT,
        `date` DATE NOT NULL,
        `start_time` TIME NOT NULL,
        `end_time` TIME NOT NULL
    );

CREATE TABLE
    `ShiftBookings` (
        `shift_id` INTEGER NOT NULL,
        `user_id` INTEGER NOT NULL,
        PRIMARY KEY (`shift_id`, `user_id`)
    );

CREATE TABLE
    `SummarySheets` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT,
        `service_id` INTEGER NOT NULL,
        `owner_id` INTEGER NOT NULL,
        FOREIGN KEY (`service_id`) REFERENCES `Services` (`id`),
        FOREIGN KEY (`owner_id`) REFERENCES `Users` (`id`)
    );

CREATE TABLE
    `Tasks` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT,
        `sumsheet_id` INTEGER NOT NULL,
        `kitchenproc_id` INTEGER NOT NULL,
        `preparation_id` INTEGER DEFAULT NULL,
        `description` TEXT,
        `type` INTEGER NOT NULL DEFAULT 1,
        `quantity` REAL DEFAULT NULL,
        `position` INTEGER NOT NULL DEFAULT 0,
        `portions` INTEGER DEFAULT NULL,
        `ready` INTEGER DEFAULT 0,
        FOREIGN KEY (`sumsheet_id`) REFERENCES `SummarySheets` (`id`),
        FOREIGN KEY (`kitchenproc_id`) REFERENCES `Recipes` (`id`)
    );

CREATE TABLE
    `Assignment` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT,
        `sumsheet_id` INTEGER NOT NULL,
        `task_id` INTEGER NOT NULL,
        `cook_id` INTEGER NOT NULL,
        `shift_id` INTEGER NOT NULL,
        FOREIGN KEY (`sumsheet_id`) REFERENCES `SummarySheets` (`id`),
        FOREIGN KEY (`task_id`) REFERENCES `Tasks` (`id`),
        FOREIGN KEY (`cook_id`) REFERENCES `Users` (`id`),
        FOREIGN KEY (`shift_id`) REFERENCES `Shifts` (`id`)
    );

-- Clean up existing data
DELETE FROM RecipePreparations
WHERE
    recipe_id > 0;

DELETE FROM Preparations
WHERE
    id > 0;

DELETE FROM Recipes
WHERE
    id > 0;

-- Reset auto-increment counters
DELETE FROM sqlite_sequence
WHERE
    name IN ('Recipes', 'Preparations');

INSERT INTO
    Preparations (name, description)
VALUES
    (
        'Impasto per pasta',
        'Preparazione base per pasta fresca'
    ),
    (
        'Impasto per pizza',
        'Impasto lievitato per pizza'
    ),
    (
        'Ragù alla bolognese',
        'Sugo di carne tipico emiliano'
    ),
    (
        'Besciamella',
        'Salsa bianca a base di burro, farina e latte'
    ),
    (
        'Pasta sfoglia',
        'Impasto a strati per preparazioni dolci e salate'
    ),
    (
        'Soffritto',
        'Base aromatica di cipolla, carota e sedano'
    ),
    (
        'Brodo vegetale',
        'Liquido di cottura a base di verdure'
    ),
    (
        'Brodo di carne',
        'Liquido di cottura a base di carne'
    ),
    (
        'Salsa di pomodoro',
        'Sugo base di pomodoro e basilico'
    ),
    (
        'Crema pasticcera',
        'Crema dolce per ripieni di pasticceria'
    ),
    (
        'Sugo all''amatriciana',
        'Sugo a base di guanciale, pomodoro e pecorino'
    ),
    (
        'Pesto alla genovese',
        'Salsa a base di basilico e pinoli'
    ),
    (
        'Salsa carbonara',
        'Condimento a base di uova, guanciale e pecorino'
    ),
    (
        'Gremolata',
        'Condimento aromatico a base di prezzemolo, aglio e limone'
    ),
    (
        'Marinatura per pesce',
        'Preparazione di olio, limone ed erbe per pesce'
    ),
    (
        'Marinatura per carne',
        'Preparazione di erbe, vino e spezie per carne'
    ),
    (
        'Pangrattato aromatizzato',
        'Pane grattugiato con erbe e spezie'
    ),
    (
        'Crema al mascarpone',
        'Base per dolci a base di mascarpone e uova'
    ),
    (
        'Pasta brisée',
        'Impasto base per crostate salate'
    ),
    (
        'Pasta frolla',
        'Impasto dolce per crostate e biscotti'
    );

-- Insert recipes without explicit IDs
INSERT INTO
    Recipes (name, description)
VALUES -- ANTIPASTI
    (
        'Bruschetta al Pomodoro',
        'Fette di pane casereccio tostate con pomodoro, aglio, basilico e olio extravergine d''oliva'
    ),
    (
        'Caprese',
        'Insalata con mozzarella di bufala, pomodori e basilico condita con olio e aceto balsamico'
    ),
    (
        'Arancini di Riso',
        'Crocchette di riso ripiene di ragù, piselli e mozzarella, fritte'
    ),
    (
        'Prosciutto e Melone',
        'Fette di melone avvolte con prosciutto crudo'
    ),
    (
        'Caponata Siciliana',
        'Piatto agrodolce siciliano con melanzane, sedano, olive e capperi'
    ),
    -- PRIMI PIATTI
    (
        'Spaghetti alla Carbonara',
        'Pasta condita con uova, guanciale, pecorino romano e pepe nero'
    ),
    (
        'Risotto ai Funghi Porcini',
        'Riso cremoso con funghi porcini e parmigiano'
    ),
    (
        'Lasagne alla Bolognese',
        'Pasta a strati con ragù, besciamella e parmigiano reggiano'
    ),
    (
        'Tagliatelle al Ragù',
        'Pasta all''uovo con tradizionale sugo di carne'
    ),
    (
        'Pasta alla Norma',
        'Pasta con melanzane fritte, pomodoro, ricotta salata e basilico'
    ),
    (
        'Gnocchi di Patate al Pesto',
        'Gnocchi di patate conditi con pesto alla genovese'
    ),
    (
        'Spaghetti all''Amatriciana',
        'Pasta con sugo a base di guanciale, pomodoro e pecorino'
    ),
    -- SECONDI PIATTI
    (
        'Ossobuco alla Milanese',
        'Stinco di vitello brasato con gremolata, servito con risotto giallo'
    ),
    (
        'Saltimbocca alla Romana',
        'Fettine di vitello con prosciutto e salvia'
    ),
    (
        'Bistecca alla Fiorentina',
        'Bistecca di manzo alta con osso cotta sulla griglia'
    ),
    (
        'Pollo alla Cacciatora',
        'Pollo in umido con pomodoro, cipolle, erbe e vino'
    ),
    (
        'Melanzane alla Parmigiana',
        'Melanzane a strati con pomodoro, mozzarella e parmigiano'
    ),
    (
        'Pesce al Forno con Patate',
        'Pesce al forno con patate, pomodorini ed erbe aromatiche'
    ),
    (
        'Vitello Tonnato',
        'Fettine di vitello con salsa al tonno e capperi'
    ),
    -- CONTORNI
    (
        'Patate al Rosmarino',
        'Patate arrosto con rosmarino e aglio'
    ),
    (
        'Verdure Grigliate',
        'Verdure di stagione grigliate e condite con olio d''oliva'
    ),
    (
        'Insalata Mista',
        'Insalata verde con pomodori, carote e cetrioli'
    ),
    (
        'Fagioli all''Uccelletto',
        'Fagioli in umido con salvia e pomodoro'
    ),
    -- DOLCI
    (
        'Tiramisù',
        'Dolce al cucchiaio con mascarpone, caffè e savoiardi'
    ),
    (
        'Panna Cotta',
        'Dolce a base di panna cotta servito con frutta o caramello'
    ),
    (
        'Cannoli Siciliani',
        'Cialde fritte ripiene di crema di ricotta e gocce di cioccolato'
    ),
    (
        'Gelato Artigianale',
        'Gelato in vari gusti fatto con ingredienti freschi'
    ),
    (
        'Crostata di Frutta',
        'Torta con base di pasta frolla, crema pasticcera e frutta fresca'
    ),
    -- PIZZE
    (
        'Pizza Margherita',
        'Pizza classica con pomodoro, mozzarella e basilico'
    ),
    (
        'Pizza Quattro Formaggi',
        'Pizza con mozzarella, gorgonzola, fontina e parmigiano'
    ),
    (
        'Pizza Diavola',
        'Pizza piccante con salame piccante'
    ),
    (
        'Pizza Marinara',
        'Pizza semplice con pomodoro, aglio e origano'
    ),
    -- SPECIALITÀ REGIONALI
    (
        'Polenta con Funghi',
        'Polenta cremosa servita con funghi trifolati'
    ),
    (
        'Ribollita Toscana',
        'Zuppa di pane toscana con verdure e fagioli'
    ),
    (
        'Arrosticini Abruzzesi',
        'Spiedini di carne di pecora alla brace tipici dell''Abruzzo'
    ),
    (
        'Canederli Tirolesi',
        'Gnocchi di pane del Trentino-Alto Adige'
    ),
    (
        'Bagna Càuda Piemontese',
        'Salsa calda a base di aglio, acciughe e olio con verdure crude'
    ),
    (
        'Risotto alla Milanese',
        'Risotto allo zafferano tipico milanese'
    ),
    (
        'Pesto alla Trapanese',
        'Pesto siciliano con pomodoro, mandorle e basilico'
    );

-- Now associate recipes with preparations using variables
-- We'll use a more direct approach without temporary views
-- Bruschetta al Pomodoro with Salsa di pomodoro
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Bruschetta al Pomodoro'
    AND p.name = 'Salsa di pomodoro';

-- Arancini di Riso with Ragù alla bolognese
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Arancini di Riso'
    AND p.name = 'Ragù alla bolognese';

-- Caponata Siciliana with Soffritto
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Caponata Siciliana'
    AND p.name = 'Soffritto';

-- Spaghetti alla Carbonara with Salsa carbonara
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Spaghetti alla Carbonara'
    AND p.name = 'Salsa carbonara';

-- Risotto ai Funghi Porcini with Brodo vegetale
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Risotto ai Funghi Porcini'
    AND p.name = 'Brodo vegetale';

-- Lasagne alla Bolognese with its three preparations
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Lasagne alla Bolognese'
    AND p.name = 'Ragù alla bolognese';

INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Lasagne alla Bolognese'
    AND p.name = 'Besciamella';

INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Lasagne alla Bolognese'
    AND p.name = 'Impasto per pasta';

-- Tagliatelle al Ragù with its two preparations
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Tagliatelle al Ragù'
    AND p.name = 'Ragù alla bolognese';

INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Tagliatelle al Ragù'
    AND p.name = 'Impasto per pasta';

-- Pasta alla Norma with Salsa di pomodoro
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Pasta alla Norma'
    AND p.name = 'Salsa di pomodoro';

-- Gnocchi di Patate al Pesto with its two preparations
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Gnocchi di Patate al Pesto'
    AND p.name = 'Impasto per pasta';

INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Gnocchi di Patate al Pesto'
    AND p.name = 'Pesto alla genovese';

-- Spaghetti all'Amatriciana with Sugo all'amatriciana
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Spaghetti all''Amatriciana'
    AND p.name = 'Sugo all''amatriciana';

-- Ossobuco alla Milanese with its two preparations
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Ossobuco alla Milanese'
    AND p.name = 'Gremolata';

INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Ossobuco alla Milanese'
    AND p.name = 'Brodo di carne';

-- Saltimbocca alla Romana with Marinatura per carne
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Saltimbocca alla Romana'
    AND p.name = 'Marinatura per carne';

-- Pollo alla Cacciatora with its two preparations
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Pollo alla Cacciatora'
    AND p.name = 'Soffritto';

INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Pollo alla Cacciatora'
    AND p.name = 'Salsa di pomodoro';

-- Melanzane alla Parmigiana with Salsa di pomodoro
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Melanzane alla Parmigiana'
    AND p.name = 'Salsa di pomodoro';

-- Pesce al Forno con Patate with Marinatura per pesce
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Pesce al Forno con Patate'
    AND p.name = 'Marinatura per pesce';

-- Vitello Tonnato with Marinatura per carne
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Vitello Tonnato'
    AND p.name = 'Marinatura per carne';

-- Tiramisù with Crema al mascarpone
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Tiramisù'
    AND p.name = 'Crema al mascarpone';

-- Cannoli Siciliani with Crema pasticcera
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Cannoli Siciliani'
    AND p.name = 'Crema pasticcera';

-- Crostata di Frutta with its two preparations
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Crostata di Frutta'
    AND p.name = 'Pasta frolla';

INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Crostata di Frutta'
    AND p.name = 'Crema pasticcera';

-- Pizza Margherita with its two preparations
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Pizza Margherita'
    AND p.name = 'Impasto per pizza';

INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Pizza Margherita'
    AND p.name = 'Salsa di pomodoro';

-- Pizza Quattro Formaggi with Impasto per pizza
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Pizza Quattro Formaggi'
    AND p.name = 'Impasto per pizza';

-- Pizza Diavola with its two preparations
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Pizza Diavola'
    AND p.name = 'Impasto per pizza';

INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Pizza Diavola'
    AND p.name = 'Salsa di pomodoro';

-- Pizza Marinara with its two preparations
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Pizza Marinara'
    AND p.name = 'Impasto per pizza';

INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Pizza Marinara'
    AND p.name = 'Salsa di pomodoro';

-- Polenta con Funghi with Brodo vegetale
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Polenta con Funghi'
    AND p.name = 'Brodo vegetale';

-- Ribollita Toscana with Soffritto
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Ribollita Toscana'
    AND p.name = 'Soffritto';

-- Canederli Tirolesi with Brodo di carne
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Canederli Tirolesi'
    AND p.name = 'Brodo di carne';

-- Risotto alla Milanese with Brodo vegetale
INSERT INTO
    RecipePreparations (recipe_id, preparation_id)
SELECT
    r.id,
    p.id
FROM
    Recipes r,
    Preparations p
WHERE
    r.name = 'Risotto alla Milanese'
    AND p.name = 'Brodo vegetale';

-- ===== ADD CUSTOM USERS WITH ROLES =====
-- First, add users
INSERT INTO Users (username) VALUES 
('Marco'),      -- ID 1
('Giulia'),   -- ID 2
('Luca'),       -- ID 3
('Sofia'),      -- ID 4
('Antonio'), -- ID 5
('Chiara'),   -- ID 6
('Giovanni'),   -- ID 7
('Francesca'); -- ID 8

-- Next, set up the role entries in Roles table (if not already present)
INSERT OR IGNORE INTO Roles (id, role) VALUES
(0,'CUOCO'),
(1, 'CHEF'),
(2, 'ORGANIZZATORE'),
(3, 'SERVIZIO');

-- Now assign roles to users
-- Staff (SERVIZIO)
INSERT INTO UserRoles (user_id, role_id) VALUES
(1, 3),  -- Marco is wait staff
(2, 3);  -- Giulia is wait staff

-- Cooks (CUOCO)
INSERT INTO UserRoles (user_id, role_id) VALUES
(3, 0),    -- Luca is a cook
(4, 0);    -- Sofia is a cook

-- Chefs (CHEF)
INSERT INTO UserRoles (user_id, role_id) VALUES
(5, 1),     -- Antonio is a chef
(6, 1);     -- Chiara is a chef

-- Organizers (ORGANIZZATORE)
INSERT INTO UserRoles (user_id, role_id) VALUES
(7, 2),  -- Giovanni is an organizer
(8, 2);  -- Francesca is an organizer

-- Users with multiple roles
INSERT INTO UserRoles (user_id, role_id) VALUES
(5, 0),        -- Antonio is both chef and cook
(7, 3),     -- Giovanni is both organizer and wait staff
(4, 3),     -- Sofia is both cook and wait staff
(6, 2); -- Chiara is both chef and organizer

-- First create a menu
INSERT INTO Menus (title, owner_id, published) 
VALUES ('Sample Menu', 5, 1);  -- Created by Antonio (chef), and published

-- Add three sections to the menu
INSERT INTO MenuSections (menu_id, name, position) VALUES 
(1, 'Appetizers', 0),
(1, 'Main Courses', 1),
(1, 'Desserts', 2);

INSERT INTO MenuItems (menu_id, section_id, description, recipe_id, position) 
SELECT 1, 1, 'Caprese', r.id, 0
FROM Recipes r WHERE r.name = 'Caprese';

INSERT INTO MenuItems (menu_id, section_id, description, recipe_id, position) 
SELECT 1, 1, 'Prosciutto e Melone', r.id, 0
FROM Recipes r WHERE r.name = 'Prosciutto e Melone';

INSERT INTO MenuItems (menu_id, section_id, description, recipe_id, position) 
SELECT 1, 2, 'Spaghetti alla Carbonara', r.id, 0
FROM Recipes r WHERE r.name = 'Spaghetti alla Carbonara';

INSERT INTO MenuItems (menu_id, section_id, description, recipe_id, position) 
SELECT 1, 2, 'Ossobuco alla Milanese', r.id, 0
FROM Recipes r WHERE r.name = 'Ossobuco alla Milanese';

INSERT INTO MenuItems (menu_id, section_id, description, recipe_id, position) 
SELECT 1, 3, 'Panna Cotta', r.id, 0
FROM Recipes r WHERE r.name = 'Panna Cotta';

INSERT INTO MenuItems (menu_id, section_id, description, recipe_id, position) 
SELECT 1, 3, 'Gelato Artigianale', r.id, 0
FROM Recipes r WHERE r.name = 'Gelato Artigianale';


-- ===== ADD SAMPLE EVENT AND SERVICES =====

-- Create a new event
INSERT INTO Events (name, date_start, date_end, chef_id) VALUES 
('Gala Aziendale Annuale', date('2025-06-15'), date('2025-06-16'), 5);  -- Assigned to Antonio

-- Create two services for this event
-- First service (lunch): assigned to chef Antonio (ID 5) with the existing menu (ID 1)
INSERT INTO Services (
    event_id, 
    name, 
    approved_menu_id, 
    service_date, 
    time_start, 
    time_end, 
    location
) VALUES (
    1,                          -- Event ID
    'Pranzo Buffet Aziendale',  -- Service name in Italian
    1,                          -- Approved menu (same as proposed)
    date('2025-06-15'),         -- Service date
    time('12:00'),              -- Start time
    time('15:00'),              -- End time
    'Salone Grande'             -- Location in Italian
);

-- Second service (dinner): only proposed menu, not yet approved
INSERT INTO Services (
    event_id, 
    name, 
    approved_menu_id, 
    service_date, 
    time_start, 
    time_end, 
    location
) VALUES (
    1,                          -- Event ID
    'Cena di Gala',             -- Service name in Italian
    0,                          -- Not approved yet (0)
    date('2025-06-15'),         -- Service date
    time('19:00'),              -- Start time
    time('23:00'),              -- End time
    'Sala Esecutiva'            -- Location in Italian
);