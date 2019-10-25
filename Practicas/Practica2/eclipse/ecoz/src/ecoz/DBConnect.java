//******************************************************************************
// File:   DBConnect.java
// Author: Andrés Gavín Murillo 716358
// Author: Eduardo Gimeno Soriano 721615
// Author: Sergio Álvarez Peiro 740241
// Date:   Octubre 2019
// Coms:   Sistemas de información - Práctica 2
//******************************************************************************

package ecoz;

import java.sql.*;

public abstract class DBConnect { // Capa DAO
	
	public static Connection createConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:postgresql://localhost:5432/sisinf", "sisinf", "sisinf");
	}
}
