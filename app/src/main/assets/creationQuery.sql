
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
    FOREIGN KEY(UUID) REFERENCES USUARIO(UUID)
);

CREATE TABLE MOVIMIENTOS (
    idMovimiento INTEGER PRIMARY KEY AUTOINCREMENT,
    nombreFecha TEXT,
    cantidadMovida REAL,
    esUnGasto BOOLEAN
);

CREATE TABLE CUENTA_MOVIMIENTO (
    idCuenta INTEGER,
    idMovimiento INTEGER,
    PRIMARY KEY (idCuenta, idMovimiento),
    FOREIGN KEY (idCuenta) REFERENCES CUENTA(idCuenta),
    FOREIGN KEY (idMovimiento) REFERENCES MOVIMIENTOS(idMovimiento)
);
