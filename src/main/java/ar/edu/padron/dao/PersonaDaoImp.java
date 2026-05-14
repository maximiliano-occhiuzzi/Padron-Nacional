package ar.edu.padron.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ar.edu.padron.db.ConexionDB;
import ar.edu.padron.domain.Persona;
import ar.edu.padron.enums.SexoEnum;

public class PersonaDaoImp implements PersonaDAO {

	@Override
	public int save(Persona p) throws Exception {

		Statement st = null;
		ResultSet rs = null;
		try {
			st = ConexionDB.getInstance().dameConnection().createStatement();
			int result = st.executeUpdate(
					"INSERT INTO personas (nro_documento, apellido, nombre, fecha_nacimiento, sexo, domicilio, habilitado_votar) VALUES ("
							+ "'" + p.getNroDocumento() + "', " + "'" + p.getApellido() + "', " + "'" + p.getNombre()
							+ "', " + "'" + p.getFechaNacimiento() + "', " + "'" + p.getSexo().name() + "', " + "'"
							+ p.getDomicilio() + "', " + (p.isHabilitadoVotar() ? 1 : 0) + ")");
			if (result == 0) {
				throw new Exception("No se inserto el registro");
			}
			return result;
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		} finally {
			finalizarConexion(st, rs);
		}
	}

	@Override
	public Persona findByDocumento(String dni) throws Exception {
		Persona p = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			st = ConexionDB.getInstance().dameConnection().createStatement();
			rs = st.executeQuery("SELECT * FROM personas WHERE nro_documento = '" + dni + "'");
			if (rs.next()) {
				p = new Persona();
				p.setId(rs.getInt("id"));
				p.setNroDocumento(rs.getString("nro_documento")); // Cambiado a nro_documento
				p.setApellido(rs.getString("apellido"));
				p.setNombre(rs.getString("nombre"));
				p.setFechaNacimiento(rs.getDate("fecha_nacimiento").toLocalDate()); // Cambiado a fecha_nacimiento
				p.setSexo(SexoEnum.valueOf(rs.getString("sexo")));
				p.setDomicilio(rs.getString("domicilio"));
				p.setHabilitadoVotar(rs.getBoolean("habilitado_votar")); // Cambiado a habilitado_votar
			}

			return p;
		} catch (Exception e) {
			System.out.println("Error en DAO al buscar por documento: " + e.getMessage());
			throw new Exception("Error al obtener la persona: " + e.getMessage());
		} finally {
			finalizarConexion(st, rs);
		}

	}

	@Override
	public List<Persona> findAll() throws Exception {
		Statement st = null;
		ResultSet rs = null;
		List<Persona> lista = new ArrayList<>(); // Creás la bolsa para las personas

		try {
			st = ConexionDB.getInstance().dameConnection().createStatement();
			rs = st.executeQuery("SELECT * FROM personas");

			while (rs.next()) { // El while recorre todas las filas de la tabla
				Persona p = new Persona();
				p.setId(rs.getInt("id"));
				p.setNroDocumento(rs.getString("nro_documento")); // Cambiado a nro_documento
				p.setApellido(rs.getString("apellido"));
				p.setNombre(rs.getString("nombre"));

				// Conversión de fecha y enum (igual que en findByDocumento)
				if (rs.getDate("fecha_nacimiento") != null) { // Cambiado a fecha_nacimiento
					p.setFechaNacimiento(rs.getDate("fecha_nacimiento").toLocalDate()); // Cambiado a fecha_nacimiento
				}
				p.setSexo(SexoEnum.valueOf(rs.getString("sexo")));
				p.setDomicilio(rs.getString("domicilio"));
				p.setHabilitadoVotar(rs.getBoolean("habilitado_votar")); // Cambiado a habilitado_votar

				lista.add(p); // Agregás la persona a la lista
			}
		} catch (Exception e) {
			throw new Exception("Error al listar el padrón: " + e.getMessage());
		} finally {
			finalizarConexion(st, rs);
		}
		return lista;
	}

	@Override
	public boolean existeDocumento(String dni) throws Exception {
		// Si findByDocumento encuentra a alguien, devuelve el objeto (no es null)
		// Si no lo encuentra, devuelve null.
		return findByDocumento(dni) != null;
	}

	@Override
	public boolean updateHabilitado(int id, boolean habilitado) throws Exception {
		Statement st = null;
		ResultSet rs = null;
		try {
			st = ConexionDB.getInstance().dameConnection().createStatement();
			int valorHabilitado = habilitado ? 1 : 0;
			String sql = "UPDATE personas SET habilitado_votar = " + valorHabilitado + " WHERE id = " + id; // Cambiado
																											// a
																											// habilitado_votar

			int result = st.executeUpdate(sql);
			return result > 0;

		} catch (Exception e) {
			System.out.println("Error al actualizar estado: " + e.getMessage());
			throw new Exception("No se pudo cambiar el estado de habilitación: " + e.getMessage());
		} finally {
			finalizarConexion(st, rs);
		}
	}

	@Override
	public boolean saveImagen(int personaId, byte[] img, String nombre) throws Exception {
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			// Ajustado a los nombres de tu tabla: persona_id, contenido, nombre_archivo
			String sql = "INSERT INTO imagenes_dni (persona_id, contenido, nombre_archivo) VALUES (?, ?, ?)";

			ps = ConexionDB.getInstance().dameConnection().prepareStatement(sql);

			ps.setInt(1, personaId);
			ps.setBytes(2, img); // El byte[] va a 'contenido'
			ps.setString(3, nombre); // El String va a 'nombre_archivo'

			int result = ps.executeUpdate();
			return result > 0;

		} catch (Exception e) {
			throw new Exception("Error al guardar en imagenes_dni: " + e.getMessage());
		} finally {
			finalizarConexion(ps, null);
		}
	}

	@Override
	public byte[] getImagen(int personaId) throws Exception {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = ConexionDB.getInstance().dameConnection()
					.prepareStatement("SELECT contenido FROM imagenes_dni WHERE persona_id = ?");
			ps.setInt(1, personaId);
			rs = ps.executeQuery();

			if (rs.next()) {
				return rs.getBytes("contenido"); // Recuperamos los bytes para mostrar la foto
			}
		} finally {
			finalizarConexion(ps, rs);
		}
		return null;
	}

	private void finalizarConexion(Statement st, ResultSet rs) {
		try {
			if (st != null) {
				st.close();
			}
			if (rs != null) { // Esta validación evita el NullPointerException
				rs.close();
			}
		} catch (SQLException e) {
			System.out.println("Error al cerrar recursos: " + e.getMessage());
		}
	}
}
