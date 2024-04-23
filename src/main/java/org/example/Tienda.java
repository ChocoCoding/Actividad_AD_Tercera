package org.example;

import com.mongodb.MongoSocketOpenException;
import com.mongodb.client.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.basex.build.json.JsonParser;
import org.basex.examples.api.BaseXClient;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.utilities.Utilities;

import java.io.IOException;
import java.net.ConnectException;
import java.util.*;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Updates.set;

public class Tienda {
    static Scanner sc = new Scanner(System.in);
    static final String nombreBdXML = "productos";
    static final String nombreBdMongo = "tienda";

    //MongoDB
    static final String colClientes = "clientes";
    static final String colCarrito = "carrito";
    static final String colPedidos = "pedidos";

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> coleccion;

    private static String idCliente;


    public static void main(String[] args) {
        int opt;


        try (BaseXClient session = new BaseXClient("localhost", 1984, "admin", "root")) {
            mongoClient = MongoClients.create("mongodb://localhost:27017");
            crearBD();
            insertarDatos();
            do {
                opt = Utilities.pedirInt("""
                        ************************************
                        1.- Consultas base de datos XML
                        2.- Consultas base de datos MongoDB
                        ************************************   
                        """);
                switch (opt) {
                    case 1:
                        menuConsultasXML(session);
                        break;
                    case 2:
                        menuConsultasMongo();
                        break;
                    case 0:
                        System.out.println();
                        break;
                    default:
                        System.out.println("La opcion no es correcta");
                }

            } while (opt != 0);
        } catch (MongoSocketOpenException | ConnectException msoe) {
            System.out.println("Error en la conexion la BD de Mongo o BaseX");
        } catch (IOException e) {
            System.out.println("Error en la conexion con la BD de BaseX");
        }


    }

    private static void menuConsultasMongo() {
        int opt;

        do {
            opt = Utilities.pedirInt("""
                    **********************************************************************************************************************************************************************
                    1.- Crear un nuevo cliente (no podrá haber email repetidos).
                    2.- Iniciar Sesión con Email.
                    3.- Borrar un cliente.
                    4.- Teniendo en cuenta todos los clientes, calcular el total de la compra para cada carrito y listar los resultados ordenados por el total de forma ascendente.
                    5.- Teniendo en cuenta todos los clientes, obtener el total gastado por cada cliente en todos sus pedidos.
                    0.- SALIR
                    ***********************************************************************************************************************************************************************  
                    """);
            switch (opt) {
                case 1:
                    crearNuevoCliente();
                    break;
                case 2:
                    iniciarSesion();
                    break;
                case 3:
                    borrarCliente();
                    break;
                case 4:
                    mostrarPrecioTodosLosCarritos();
                    break;
                case 5:
                    mostrarTotalGastadoPorClientePedidos();
                case 0:
                    System.out.println();
                    break;
                default:
                    System.out.println("La opcion no es correcta");
            }

        } while (opt != 0);
    }

    private static void mostrarTotalGastadoPorClientePedidos() {
        seleccionarColeccion(colPedidos);
        var pedidos = Arrays.asList(
                unwind("$productos"),
                set("productos.total_producto", new Document("$multiply",
                        Arrays.asList("$productos.cantidad", "$productos.precio_unitario"))),
                group("$cliente_id", sum("total_gastado", "$productos.total_producto"))
        );

        List<Document> results = coleccion.aggregate(pedidos).into(new ArrayList<>());

        for (Document result : results) {
            ObjectId clienteId = result.getObjectId("_id");
            double totalGastado = result.getDouble("total_gastado");

            System.out.println("Cliente ID: " + clienteId);
            System.out.println("Total Gastado: " + totalGastado);
            System.out.println("-----------------------");
        }

    }

    private static void borrarCliente() {
        seleccionarColeccion(colClientes);
        String email = Utilities.pedirString("Introduce el email del cliente a borrar");

        DeleteResult dr = coleccion.deleteOne(new Document("email", email));

        if (dr.getDeletedCount() > 0) {
            System.out.println("El cliente ha sido eliminado");
        } else System.out.println("No se ha encontrado un cliente con email: " + email);


    }

    private static void iniciarSesion() {
        seleccionarColeccion(colClientes);

        String email = Utilities.pedirString("Introduce el email del cliente");
        Document filter = new Document("email", email);
        Document projection = new Document("_id", 1);

        FindIterable<Document> result = coleccion.find(filter).projection(projection);

        if (result.first() != null) {
            Object id = result.first().get("_id");
            idCliente = id.toString();
            System.out.println("La id del cliente es: " + idCliente);
            menuSesionCliente();
        } else System.out.println("El cliente con email " + email + " no existe");
    }

    private static void menuSesionCliente() {
        int opt;

        do {
            opt = Utilities.pedirInt("" +
                    "\"******************************************** Bienvenido Cliente: " + idCliente + " *************************************************************************\n" +
                    " 1.- Modificar el valor de un campo de la información del cliente.\n" +
                    " 2.- Añadir producto al carrito del cliente. Se pedirá: id del producto y cantidad, así como si se desea seguir introduciendo más productos.\n" +
                    " 3.- Mostrar el carrito del cliente. Se mostrarán los datos del carrito y el precio total.\n" +
                    " 4.- Mostrar pedidos del cliente.\n" +
                    " 5.- Pagar el carrito de un cliente: se mostrará el carrito junto con una orden de confirmación. Si es positiva los productos pasarán a formar parte de un nuevo pedido.\n" +
                    " 0.- Cerrar Sesión\n" +
                    " **********************************************************************************************************************************************************************");

            switch (opt) {
                case 1:
                    modificarElValorDeUnCampos();
                    break;
                case 2:
                    addProductoCarrito();
                    break;
                case 3:
                    mostrarCarritoCliente();
                    break;
                case 4:
                    mostrarPedidosCliente();
                    break;
                case 5:
                    pagarCarrito();
                    break;

            }

        } while (opt != 0);


    }

    private static void mostrarPrecioTodosLosCarritos() {
        seleccionarColeccion(colCarrito);

        var carritos = Arrays.asList(
                unwind("$productos"),
                set("productos.total_producto", new Document("$multiply",
                        Arrays.asList("$productos.cantidad", "$productos.precio_unitario"))),
                group("$cliente_id", sum("total_compra", "$productos.total_producto")),
                sort(ascending("total_compra"))
        );

        List<Document> results = coleccion.aggregate(carritos).into(new ArrayList<>());

        for (Document result : results) {
            ObjectId clienteId = result.getObjectId("_id");
            double totalCompra = result.getDouble("total_compra");

            System.out.println("Cliente ID: " + clienteId);
            System.out.println("Total de compra: " + totalCompra);
            System.out.println("-----------------------");
        }
    }

    private static void pagarCarrito() {
        seleccionarColeccion(colCarrito);

        Document carrito = coleccion.find(new Document("cliente_id", new ObjectId(idCliente))).first();

        if (carrito != null) {
            mostrarCarritoCliente();

            String conf = Utilities.pedirString("¿Deseas hacer la compra? s/n");

            if (conf.equalsIgnoreCase("s")) {
                Object carritoId = carrito.get("_id");

                Document nuevoPedido = new Document();

                nuevoPedido.append("cliente_id", new ObjectId(idCliente))
                        .append("productos", carrito.get("productos"))
                        .append("total", calcularTotalCarrito());

                seleccionarColeccion(colPedidos);
                coleccion.insertOne(nuevoPedido);

                System.out.println("Pedido confirmado. ¡Gracias por su compra!");

                ArrayList<Document> listaVacia = new ArrayList<>();

                seleccionarColeccion(colCarrito);
                Document updateQuery = new Document("$set", new Document("productos", listaVacia));
                coleccion.updateOne(new Document("_id", carritoId), updateQuery);

            } else System.out.println("Pedido cancelado");
        } else {
            System.out.println("El cliente no tiene ningún producto en el carrito.");
        }

    }

    private static void mostrarPedidosCliente() {
        seleccionarColeccion(colPedidos);
        Iterable<Document> pedidosCliente = coleccion.find(new Document("cliente_id", new ObjectId(idCliente)));
        int cont =0 ;
        if (pedidosCliente.iterator().hasNext()) {

            System.out.println("Pedidos del Cliente:");
            for (Document pedido : pedidosCliente) {
                cont++;
                System.out.println("Pedido:" + cont);
                for (Map.Entry<String, Object> entry : pedido.entrySet()) {
                    if (entry.getKey().equals("productos")){
                        System.out.println(entry.getKey() + ":");
                        ArrayList<Document> productos = (ArrayList<Document>) entry.getValue();
                        for (Document d : productos){
                            for (Map.Entry<String, Object> prodEntry : d.entrySet()) {
                                if (prodEntry.getKey().equals("precio_unitario")){
                                    System.out.println("\t\t" + prodEntry.getKey() + ": " + prodEntry.getValue());
                                    System.out.println();
                                }else  System.out.println("\t\t" + prodEntry.getKey() + ": " + prodEntry.getValue());


                        }
                    }
                }else System.out.println("\t" + entry.getKey() + ": " + entry.getValue());
            }
        }
        }else {
            System.out.println("El cliente no tiene ningún pedido.");
        }

    }

    private static void mostrarCarritoCliente() {
        seleccionarColeccion(colCarrito);

        System.out.println("Precio Total del Carrito: $" + calcularTotalCarrito());
    }

    private static Double calcularTotalCarrito() {
        Document carritoCliente = coleccion.find(new Document("cliente_id", new ObjectId(idCliente))).first();
        double precioTotalProductos = 0.0;
        if (carritoCliente != null) {
            for (Map.Entry<String, Object> entry : carritoCliente.entrySet()) {
                if (entry.getKey().equals("productos")) {
                    System.out.println(entry.getKey() + ": ");
                    ArrayList<Document> productos = (ArrayList<Document>) entry.getValue();
                    for (Document prod : productos) {
                        System.out.println("\tProducto:");
                        for (Map.Entry<String, Object> prodEntry : prod.entrySet()) {
                            System.out.println("\t\t" + prodEntry.getKey() + ": " + prodEntry.getValue());
                        }
                    }
                } else {
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                }
            }

            for (Document prod : (Iterable<Document>) carritoCliente.get("productos")) {
                double precioUnitario = prod.getDouble("precio_unitario");
                int cantidad = prod.getInteger("cantidad");
                precioTotalProductos += precioUnitario * cantidad;
            }
        } else System.err.println("El cliente no tiene carrito");
        return precioTotalProductos;
    }

    private static void addProductoCarrito() {
        seleccionarColeccion(colCarrito);
        String seguir = "s";
        
        do {
            try {
            int idProducto = Utilities.pedirInt("Introduce la id del producto");
            int cantidad = Utilities.pedirInt("Introduce la cantidad");

            ArrayList<String> datosProducto = conseguirDatosProducto(idProducto);

            Document nuevoProducto = new Document()
                    .append("producto_id", idProducto)
                    .append("nombre", datosProducto.get(0))
                    .append("cantidad", cantidad)
                    .append("precio_unitario", Double.parseDouble(datosProducto.get(1)));


            Document filtro = new Document("cliente_id", new ObjectId(idCliente));

            Document update = new Document("$push", new Document("productos", nuevoProducto));

            coleccion.updateOne(new Document(filtro), update);

            System.out.println("Producto agregado al carrito");

            seguir = Utilities.pedirString("¿Quieres insertar mas productos? s/n");
        }catch (IndexOutOfBoundsException ioobe){
                System.err.println("ERROR!!! La id del producto no existe");
            }
        } while (!seguir.equalsIgnoreCase("n"));
     
    }

    private static ArrayList<String> conseguirDatosProducto(int id) {
        try (BaseXClient session = new BaseXClient("localhost", 1984, "admin", "root")) {
            String con = String.format("for $producto in db:get('productos')/productos/producto[id=" + id + "] return ($producto/nombre/text(), $producto/precio/text())");
            ArrayList<String> datos = new ArrayList<>();

            try {
                BaseXClient.Query query = session.query(con);

                while (query.more()) {
                    datos.add(query.next());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return datos;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void modificarElValorDeUnCampos() {
        seleccionarColeccion(colClientes);

        String campoUpdate = Utilities.pedirString("Introduce el campo que quieres cambiar");

        String nuevoValor = Utilities.pedirString("Introduce el nuevo valor para ese campo");


        Document filter = new Document("_id", new ObjectId(idCliente));

        UpdateResult ur = coleccion.updateMany(filter, new Document("$set", new Document(campoUpdate, nuevoValor)));

        if (ur.getMatchedCount() > 0) {
            System.out.println("Se han actualizado los campos");
        } else System.out.println("Ha ocurrido un error al actualizar los campos");
    }

    private static void crearNuevoCliente() {
        seleccionarColeccion(colClientes);

        String email = Utilities.pedirString("Introduce el nuevo email que deseas para el cliente");

        FindIterable<Document> iterDoc = coleccion.find(new Document("email", email));

        if (iterDoc.first() != null) {
            System.out.println("El mail ya existe");
        } else {
            String nombre = Utilities.pedirString("Introduce tu nombre");
            String direccion = Utilities.pedirString("Introduce tu direccion");
            Document nuevoCliente = new Document("nombre", nombre).append("email", email).append("direccion", direccion);
            coleccion.insertOne(nuevoCliente);

            seleccionarColeccion(colCarrito);
            ArrayList<Document> listaVacia = new ArrayList<>();
            Document carritoNuevo =
                    new Document("_id", new ObjectId())
                            .append("cliente_id", nuevoCliente.get("_id"))
                            .append("productos", listaVacia);
            coleccion.insertOne(carritoNuevo);
        }
    }

    private static void insertarDatos() {
        seleccionarColeccion(colClientes);
        if (coleccion.countDocuments() == 0) {

            Document producto1 = new Document("producto_id", 1)
                    .append("nombre", "Laptop HP Pavilion")
                    .append("cantidad", 1)
                    .append("precio_unitario", 799.99);

            Document producto2 = new Document("producto_id", 2)
                    .append("nombre", "Smartphone Samsung Galaxy S21")
                    .append("cantidad", 1)
                    .append("precio_unitario", 899.99);

            Document producto3 = new Document("producto_id", 3)
                    .append("nombre", "Tablet Lenovo Tab M10")
                    .append("cantidad", 1)
                    .append("precio_unitario", 199.99);

            Document producto4 = new Document("producto_id", 4)
                    .append("nombre", "Auriculares Inalámbricos Sony WH-1000XM4")
                    .append("cantidad", 1)
                    .append("precio_unitario", 299.99);

            Document producto5 = new Document("producto_id", 5)
                    .append("nombre", "Televisor 4K con tecnología Quantum Dot y pantalla de 55 pulgadas.")
                    .append("cantidad", 1)
                    .append("precio_unitario", 1299.99);

            List<Document> clientes = new ArrayList<>();
            clientes.add(new Document("_id", new ObjectId())
                    .append("nombre", "Nombre Cliente 1")
                    .append("email", "cliente1@example.com")
                    .append("direccion", "Dirección Cliente 1"));
            clientes.add(new Document("_id", new ObjectId())
                    .append("nombre", "Nombre Cliente 2")
                    .append("email", "cliente2@example.com")
                    .append("direccion", "Dirección Cliente 2"));
            clientes.add(new Document("_id", new ObjectId())
                    .append("nombre", "Nombre Cliente 3")
                    .append("email", "cliente3@example.com")
                    .append("direccion", "Dirección Cliente 3"));
            clientes.add(new Document("_id", new ObjectId())
                    .append("nombre", "Nombre Cliente 4")
                    .append("email", "cliente4@example.com")
                    .append("direccion", "Dirección Cliente 4"));
            coleccion.insertMany(clientes);

            seleccionarColeccion(colCarrito);
            List<Document> carritos = new ArrayList<>();

            carritos.add(new Document("_id", new ObjectId())
                    .append("cliente_id", clientes.get(0).getObjectId("_id"))
                    .append("productos", Arrays.asList(producto1, producto2)));

            carritos.add(new Document("_id", new ObjectId())
                    .append("cliente_id", clientes.get(1).getObjectId("_id"))
                    .append("productos", Arrays.asList(producto2, producto3, producto4)));

            carritos.add(new Document("_id", new ObjectId())
                    .append("cliente_id", clientes.get(2).getObjectId("_id"))
                    .append("productos", Arrays.asList(producto5, producto1)));

            carritos.add(new Document("_id", new ObjectId())
                    .append("cliente_id", clientes.get(3).getObjectId("_id"))
                    .append("productos", Arrays.asList(producto3)));


            coleccion.insertMany(carritos);

            seleccionarColeccion(colPedidos);

            List<Document> pedidos = new ArrayList<>();
            pedidos.add(new Document("_id", new ObjectId())
                    .append("cliente_id", clientes.get(0).getObjectId("_id"))
                    .append("productos", new Document("producto_id", 1)
                            .append("nombre", "Laptop HP Pavilion")
                            .append("cantidad", 2)
                            .append("precio_unitario", 799.99))
                    .append("total", 1599.98)
                    .append("fecha_pedido", "2023-01-01T12:30:00.000Z"));
            pedidos.add(new Document("_id", new ObjectId())
                    .append("cliente_id", clientes.get(1).getObjectId("_id"))
                    .append("productos", new Document("producto_id", 2)
                            .append("nombre", "Smartphone Samsung Galaxy S21")
                            .append("cantidad", 1)
                            .append("precio_unitario", 899.99))
                    .append("total", 899.99)
                    .append("fecha_pedido", "2023-01-02T13:45:00.000Z"));
            coleccion.insertMany(pedidos);
        }
    }

    private static void seleccionarColeccion(String nombreColeccion) {
        if (database == null)
            crearBD();
        try {
            coleccion = database.getCollection(nombreColeccion);
        } catch (Exception e) {
            database.createCollection(nombreColeccion);
            coleccion = database.getCollection(nombreColeccion);
        }

    }

    private static void crearBD() {
        database = mongoClient.getDatabase("tienda");
    }

    //********************* BASEX ************************
    private static void menuConsultasXML(BaseXClient session) {
        int opt;

        do {
            opt = Utilities.pedirInt("""
                    **********************************************************************************************************************************************************************
                    1.- Modificar el valor de un elemento de un XML según un ID.
                    2.- Eliminar un producto según su ID.
                    3.- Obtener todos los productos por orden alfabético del nombre (se mostrarán los siguientes campos: id, nombre, precio, disponibilidad y categoria).
                    4.- Listar productos con una disponibilidad mayor a X unidades (se mostrarán los siguientes campos: id, nombre, precio, disponibilidad y categoria).
                    5.- Mostrar la categoría, el nombre y el precio del producto más caro para cada categoría. En el caso de haber varios se devolverá el de la primera posición.
                    6.- Mostrar el nombre de los productos y su fabricante para aquellos productos cuya descripción incluya una subcadena.
                    7.- Mostrar la cantidad total de productos en cada categoría y calcular el porcentaje que representa respecto al total de productos.
                    0.- SALIR
                    ***********************************************************************************************************************************************************************  
                    """);
            switch (opt) {
                case 1:
                    modificarDocumento(session);
                    break;
                case 2:
                    eliminarUnNodoPorID(session);
                    break;
                case 3:
                    obtenerProductosPorOrden(session);
                    break;
                case 4:
                    mostrarProductosDisponibilidad(session);
                    break;
                case 5:
                    mostrarMasCaroPorCategoria(session);
                    break;
                case 6:
                    mostrarNombreFabricanteConSubcadena(session);
                    break;
                case 7:
                    calcularPorcentajeProductos(session);
                    break;
                case 0:
                    System.out.println();
                    break;
                default:
                    System.out.println("La opcion no es correcta");
            }

        } while (opt != 0);
    }

    private static void calcularPorcentajeProductos(BaseXClient session) {
        String con = String.format("let $totalProd := count(db:get('productos')//producto)\n" +
                "for $cat in distinct-values(db:get('productos')//producto/categoria) \n" +
                "let $prodCat := db:get('productos')//producto[categoria = $cat]\n" +
                "let $cantProdCat := count($prodCat)\n" +
                "\n" +
                "return\n" +
                "<categoria>\n" +
                "<nombre>{$cat}</nombre>\n" +
                "<cantidad>{$cantProdCat}</cantidad>\n" +
                "<porcentaje>{($cantProdCat div $totalProd) *100}</porcentaje>\n" +
                "</categoria>");

        ejecutarConsultaMultiple(session, con);

    }

    private static void mostrarNombreFabricanteConSubcadena(BaseXClient session) {
        String subcadena = Utilities.pedirString("Introduce la subcadena");

        String con = String.format("let $subcadena := '" + subcadena + "'\n" +
                "for $prod in db:get('productos')/productos/producto\n" +
                "where contains($prod/descripcion,$subcadena)\n" +
                "return\n" +
                "<producto>\n" +
                "{$prod/nombre}\n" +
                "{$prod/fabricante}\n" +
                "</producto>");

        ejecutarConsultaMultiple(session, con);


    }

    private static void mostrarMasCaroPorCategoria(BaseXClient session) {
        String con = String.format("for $cat in distinct-values(db:get('productos')/productos/producto/categoria)\n" +
                "let $prod := db:get('productos')/productos/producto[categoria = $cat][1]\n" +
                "where max($prod/precio)\n" +
                "return \n" +
                "<producto>\n" +
                "{$prod/categoria}\n" +
                "{$prod/nombre}\n" +
                "{$prod/precio}\n" +
                "</producto>");

        ejecutarConsultaMultiple(session, con);


    }

    private static void mostrarProductosDisponibilidad(BaseXClient session) {
        int disp = Utilities.pedirInt("Introduce el valor de la disponibilidad");

        String con = String.format("for $prod in db:get('productos')/productos/producto\n" +
                "where $prod/disponibilidad >" + disp + "\n" +
                "return\n" +
                "<producto>\n" +
                "{$prod/id}\n" +
                "{$prod/nombre}\n" +
                "{$prod/precio}\n" +
                "{$prod/disponibilidad}\n" +
                "{$prod/categoria}\n" +
                "</producto>");

        ejecutarConsultaMultiple(session, con);
    }

    private static void obtenerProductosPorOrden(BaseXClient session) {

        String con = String.format("for $prod in db:get('productos')/productos/producto\n" +
                "order by $prod/nombre\n" +
                "return\n" +
                "<producto>\n" +
                "{$prod/id}\n" +
                "{$prod/nombre}\n" +
                "{$prod/precio}\n" +
                "{$prod/disponibilidad}\n" +
                "{$prod/categoria}\n" +
                "</producto>");


        ejecutarConsultaMultiple(session, con);
    }

    private static void eliminarUnNodoPorID(BaseXClient session) {
        int id = Utilities.pedirInt("Indique la id del documento a eliminar: ");
        String con = String.format("delete node db:get('productos')/productos/producto[id='" + id + "']");

        if (!ejecutarConsultaSimple(session, con)){
            System.out.println("Se ha eliminado el producto " + id);
        }else System.err.println("No se ha podido eliminar el producto " + id);


    }

    private static void modificarDocumento(BaseXClient sesion) {
        int id = Utilities.pedirInt("Indique la id del documento a reemplazar: ");
        String campoModificar = Utilities.pedirString("Introduzca el campo que quiere cambiar: ");
        String nuevoValorCampo = Utilities.pedirString("Nuevo valor del campo: ");

        String con = String.format("let $prod:= db:get('productos')/productos/producto[id=%s] return replace value of node $prod/%s with '%s'", id, campoModificar, nuevoValorCampo);

        if (!ejecutarConsultaSimple(sesion, con)){
            System.out.println("Se ha actualizado " + campoModificar);
        }else System.err.println("No se ha podido actualizar " + campoModificar);
    }

    private static void ejecutarConsultaMultiple(BaseXClient session, String con) {
        try {
            BaseXClient.Query query = session.query(con);
            while (query.more()) {
                System.out.println(query.next());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean ejecutarConsultaSimple(BaseXClient session, String con) {
        try {
            BaseXClient.Query query = session.query(con);

            return query.more();

        } catch (IOException e) {
            System.err.println("El campo no existe");
            return true;
        }

    }
}



