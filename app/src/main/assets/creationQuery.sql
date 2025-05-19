CREATE TABLE USUARIO (
    UUID INTEGER PRIMARY KEY AUTOINCREMENT,
    CodigoRecuperacion TEXT,
    esUsuarioPremium BOOLEAN,
    estaSincronizado BOOLEAN,
    fechaUltimaSync DATE
);

CREATE TABLE CUENTA (
    idCuenta INTEGER PRIMARY KEY AUTOINCREMENT,
    nombreCuenta TEXT,
    balanceTotal REAL,
    monedaSeleccionada TEXT,
    fechaUltimoMovimiento DATE,
    UUID INTEGER,
    FOREIGN KEY(UUID) REFERENCES USUARIO(UUID) ON DELETE CASCADE
);

CREATE TABLE MOVIMIENTOS (
    idMovimiento INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT,
    fechaMovimiento DATE,
    cantidadMovida REAL,
    esUnGasto BOOLEAN,
    idCuenta INTEGER,
    FOREIGN KEY(idCuenta) REFERENCES CUENTA(idCuenta) ON DELETE CASCADE
);
