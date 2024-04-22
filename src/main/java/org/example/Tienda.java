package org.example;

import com.mongodb.client.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.basex.examples.api.BaseXClient;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Updates.set;

public class Tienda {
    static Scanner sc = new Scanner(System.in);
    static final String nombreBdXML = "productos";
    static final String nombreBdMongo = "tienda";

    //MongoDB
    static final String colClientes="clientes";
    static final String colCarrito="carrito";
    static final String colPedidos = "pedidos";

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> coleccion;

    private static String idCliente;


    public static void main(String[] args) {
        int opt;

        mongoClient = MongoClients.create("mongodb://localhost:27017");

        //TODO : COMPROBAR SI LA BASE DE DATOS ESTA VACIA
            crearBD();
            insertarDatos();

        try(BaseXClient session = new BaseXClient("localhost", 1984, "admin", "root")){
            do{
                opt = pedirInt("""
                    ************************************
                    1.- Consultas base de datos XML
                    2.- Consultas base de datos MongoDB
                    ************************************   
                    """);
                switch (opt){
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

            }while (opt!=0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }


    private static void menuConsultasMongo() {
        int opt;

        do{
            opt = pedirInt("""
                    **********************************************************************************************************************************************************************
                    1.- Crear un nuevo cliente (no podrá haber email repetidos).
                    2.- Iniciar Sesión con Email.
                    3.- Borrar un cliente.
                    4.- Teniendo en cuenta todos los clientes, calcular el total de la compra para cada carrito y listar los resultados ordenados por el total de forma ascendente.
                    5.- Teniendo en cuenta todos los clientes, obtener el total gastado por cada cliente en todos sus pedidos.
                    0.- SALIR
                    ***********************************************************************************************************************************************************************  
                    """);
            switch (opt){
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

        }while (opt!=0);
    }

    private static void mostrarTotalGastadoPorClientePedidos() {
        seleccionarColeccion(colPedidos);
        var pipeline = Arrays.asList(
                unwind("$productos"),
                set("productos.total_producto", new Document("$multiply",
                        Arrays.asList("$productos.cantidad", "$productos.precio_unitario"))),
                group("$cliente_id", sum("total_gastado", "$productos.total_producto"))
        );

        MongoCursor<Document> cursor = coleccion.aggregate(pipeline).iterator();
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            System.out.println(doc.toJson());
        }

    }

    private static void borrarCliente() {
        seleccionarColeccion(colClientes);
        String email = pedirString("Introduce el email del cliente a borrar");

        DeleteResult dr = coleccion.deleteOne(new Document("email",email));

        if (dr.getDeletedCount() > 0){
            System.out.println("El cliente ha sido eliminado");
        }else System.out.println("No se ha encontrado un cliente con email: " + email);


    }

    private static void iniciarSesion() {
        seleccionarColeccion(colClientes);

        String email = pedirString("Introduce el email del cliente");
        Document filter = new Document("email",email);
        Document projection = new Document("_id",1);

        FindIterable<Document> result = coleccion.find(filter).projection(projection);

        if (result.first() != null){
            Object id = result.first().get("_id");
            idCliente =  id.toString();
            System.out.println("La id del cliente es: " + idCliente);
            menuSesionCliente();
        }else System.out.println("El cliente con email " + email + " no existe");
    }
    private static void menuSesionCliente(){
        int opt;

        do {
            opt = pedirInt("" +
                    "\"******************************************** Bienvenido Cliente: "+ idCliente+" *************************************************************************\n" +
                    " 1.- Modificar el valor de un campo de la información del cliente.\n" +
                    " 2.- Añadir producto al carrito del cliente. Se pedirá: id del producto y cantidad, así como si se desea seguir introduciendo más productos.\n" +
                    " 3.- Mostrar el carrito del cliente. Se mostrarán los datos del carrito y el precio total.\n" +
                    " 4.- Mostrar pedidos del cliente.\n" +
                    " 5.- Pagar el carrito de un cliente: se mostrará el carrito junto con una orden de confirmación. Si es positiva los productos pasarán a formar parte de un nuevo pedido.\n" +
                    " 0.- Cerrar Sesión\n" +
                    " **********************************************************************************************************************************************************************");

            switch (opt){
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

        }while(opt != 0);


    }

    private static void mostrarPrecioTodosLosCarritos() {
        seleccionarColeccion(colCarrito);

        var pipeline = Arrays.asList(
                unwind("$productos"),
                set("productos.total_producto", new Document("$multiply",
                        Arrays.asList("$productos.cantidad", "$productos.precio_unitario"))),
                group("$cliente_id", sum("total_compra", "$productos.total_producto")),
                sort(ascending("total_compra"))
        );

        MongoCursor<Document> cursor = coleccion.aggregate(pipeline).iterator();
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            System.out.println(doc.toJson());
        }
    }

    private static void pagarCarrito() {
    seleccionarColeccion(colCarrito);

    Document carrito =  coleccion.find(new Document("cliente_id",new ObjectId(idCliente))).first();

    if (carrito!=null){
        System.out.println(carrito.toJson());

        String conf = pedirString("¿Deseas hacer la compra? s/n");

        if (conf.equalsIgnoreCase("s")){
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
            Document updateQuery = new Document("$set",new Document("productos", listaVacia));
            coleccion.updateOne(new Document("_id", carritoId), updateQuery);

        }else System.out.println("Pedido cancelado");
    }else {
        System.out.println("El cliente no tiene ningún producto en el carrito.");
    }

    }

    private static void mostrarPedidosCliente() {
        seleccionarColeccion(colPedidos);
        Iterable<Document> pedidosCliente = coleccion.find(new Document("cliente_id", new ObjectId(idCliente)));

        if (pedidosCliente.iterator().hasNext()) {
            // Imprimir los datos de los pedidos del cliente
            System.out.println("Pedidos del Cliente:");
            for (Document pedido : pedidosCliente) {
                System.out.println(pedido.toJson());
            }
        } else {
            System.out.println("El cliente no tiene ningún pedido.");
        }

}



    private static void mostrarCarritoCliente() {
        seleccionarColeccion(colCarrito);

        System.out.println("Precio Total del Carrito: $" + calcularTotalCarrito());
    }

    private static Double calcularTotalCarrito(){
        Document carritoCliente = coleccion.find(new Document("cliente_id",new ObjectId(idCliente))).first();
        double precioTotalProductos = 0.0;
        if (carritoCliente!=null){
            System.out.println(carritoCliente.toJson());

            for (Document prod : (Iterable<Document>) carritoCliente.get("productos")){
                double precioUnitario = prod.getDouble("precio_unitario");
                int cantidad = prod.getInteger("cantidad");
                precioTotalProductos += precioUnitario * cantidad;
            }
        }else System.out.println("El cliente no tiene carrito");
        return precioTotalProductos;
    }

    private static void addProductoCarrito() {
        seleccionarColeccion(colCarrito);
        String seguir;
        do {
            int idProducto = pedirInt("Introduce la id del producto");
            int cantidad = pedirInt("Introduce la cantidad");

            ArrayList<String> datosProducto = conseguirDatosProducto(idProducto);

            Document nuevoProducto = new Document()
                    .append("producto_id", idProducto)
                    .append("nombre",datosProducto.get(0))
                    .append("cantidad", cantidad)
                    .append("precio_unitario",Double.parseDouble(datosProducto.get(1)));


            Document filtro = new Document("cliente_id",new ObjectId(idCliente));

            Document update = new Document("$push", new Document("productos", nuevoProducto));

            coleccion.updateOne(new Document(filtro),update);

            System.out.println("Producto agregado al carrito");

            seguir = pedirString("¿Quieres insertar mas productos? s/n");
        }while(!seguir.equalsIgnoreCase("n"));
    }
    private static ArrayList<String> conseguirDatosProducto(int id) {
        try(BaseXClient session = new BaseXClient("localhost", 1984, "admin", "root")) {
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

        String campoUpdate = pedirString("Introduce el campo que quieres cambiar");

        String nuevoValor = pedirString("Introduce el nuevo valor para ese campo");


        Document filter = new Document("_id",new ObjectId(idCliente));

        UpdateResult ur = coleccion.updateMany(filter,new Document("$set",new Document(campoUpdate,nuevoValor)));

        if (ur.getMatchedCount() > 0){
            System.out.println("Se han actualizado los campos");
        }else System.out.println("Ha ocurrido un error al actualizar los campos");
    }
    private static void crearNuevoCliente() {
        seleccionarColeccion(colClientes);

        String email = pedirString("Introduce el nuevo email que deseas para el cliente");

        FindIterable<Document> iterDoc = coleccion.find(new Document("email",email));

        if (iterDoc.first()!=null){
            System.out.println("El mail ya existe");
        }else {
            String nombre = pedirString("Introduce tu nombre");
            String direccion = pedirString("Introduce tu direccion");
            Document nuevoCliente = new Document("nombre",nombre).append("email",email).append("direccion",direccion);
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
        if (coleccion.countDocuments() == 0){

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
    private static void seleccionarColeccion(String nombreColeccion){
        if (database == null)
            crearBD();
        try {
            coleccion = database.getCollection(nombreColeccion);
        }catch (Exception e){
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

        do{
            opt = pedirInt("""
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
            switch (opt){
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
                case 0:
                    System.out.println();
                    break;
                default:
                    System.out.println("La opcion no es correcta");
            }

        }while (opt!=0);
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

        try {
            BaseXClient.Query query = session.query(con);

            while (query.more()){
                System.out.println(query.next());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    private static void mostrarNombreFabricanteConSubcadena(BaseXClient session) {
        String subcadena = pedirString("Introduce la subcadena");

        String con = String.format( "let $subcadena := '" + subcadena +"'\n" +
                "for $prod in db:get('productos')/productos/producto\n" +
                "where contains($prod/descripcion,$subcadena)\n" +
                "return\n" +
                "<producto>\n" +
                "{$prod/nombre}\n" +
                "{$prod/fabricante}\n" +
                "</producto>");

            try {
                BaseXClient.Query query = session.query(con);

                while(query.more()){
                    System.out.println(query.next());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }



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

        try {
            BaseXClient.Query query = session.query(con);

            while (query.more()){
                System.out.println(query.next());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
    private static void mostrarProductosDisponibilidad(BaseXClient session) {
        int disp = pedirInt("Introduce el valor de la disponibilidad");

        String con = String.format("for $prod in db:get('productos')/productos/producto\n" +
                "where $prod/disponibilidad >" + disp +"\n" +
                "return\n" +
                "<producto>\n" +
                "{$prod/id}\n" +
                "{$prod/nombre}\n" +
                "{$prod/precio}\n" +
                "{$prod/disponibilidad}\n" +
                "{$prod/categoria}\n" +
                "</producto>");

        try {
            BaseXClient.Query query = session.query(con);

            while (query.more()){
                System.out.println(query.next());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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


            try {
                BaseXClient.Query query = session.query(con);
                while (query.more()){
                    System.out.println(query.next());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }



    }
    private static void eliminarUnNodoPorID(BaseXClient session) {
        int id = pedirInt("Indique la id del documento a eliminar: ");
        String con = String.format("delete node db:get('productos')/productos/producto[id='" + id +"']");

        try {
            BaseXClient.Query query = session.query(con);
            System.out.println(query.more());
            System.out.println(query.info());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    private static void modificarDocumento(BaseXClient sesion) {
        int id = pedirInt("Indique la id del documento a reemplazar: ");
        String campoModificar = pedirString("Introduzca el campo que quiere cambiar: ");
        String nuevoValorCampo = pedirString("Nuevo valor del campo: ");

        String con = String.format("let $prod:= db:get('productos')/productos/producto[id=%s] return replace value of node $prod/%s with '%s'",id,campoModificar,nuevoValorCampo);

        try {
            BaseXClient.Query query = sesion.query(con);

            System.out.println(query.more());
            System.out.println(query.info());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static int pedirInt(String texto) {
        while(true){
            sc = new Scanner(System.in);
            try {
                System.out.println(texto);
                return sc.nextInt();
            }catch (Exception e){
                System.out.println("ERROR, el dato introducido no es correcto");
            }
        }
    }
    private static String pedirString(String texto) {
        while(true){
            sc = new Scanner(System.in);
            try {
                System.out.println(texto);
                return sc.next();
            }catch (Exception e){
                System.out.println("ERROR, el dato introducido no es correcto");
            }
        }
    }
    private static Double pedirDouble(String texto) {
        while(true){
            sc = new Scanner(System.in);
            try {
                System.out.println(texto);
                return sc.nextDouble();
            }catch (Exception e){
                System.out.println("ERROR, el dato introducido no es correcto");
            }
        }
    }
}