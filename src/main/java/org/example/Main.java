package org.example;

import org.basex.core.cmd.CreateDB;
import org.basex.examples.api.BaseXClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    static Scanner sc = new Scanner(System.in);
    static final String nombreBdXML = "productos";
    static final String nombreBdMongo = "tienda";


    public static void main(String[] args) {

        int opt;

        try(BaseXClient session = new BaseXClient("localhost", 1984, "admin", "root")){
            //Creamos la BD
            //createBd(session);

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



    private static ArrayList<String> ejecutarConsultaMultiple(BaseXClient sesion, String consulta) {
        ArrayList<String> lista = new ArrayList<>();
        // Comprobamos si existe o si no la BD
        try(BaseXClient.Query query = sesion.query(consulta )) {

            // Comprobación e iteración de los resultados
            while(query.more()) {
                lista.add(query.next());
            }
        }catch (Exception e){}
        return lista;
    }


    private static String ejecutarConsultaUnitaria(BaseXClient session, String consulta) {
        // Comprobamos si existe o si no la BD
        try(BaseXClient.Query query = session.query(consulta)) {

            // Comprobación e iteración de los resultados
            if(query.more()) {
                return query.next();
            }
        }catch (Exception e){}
        return null;
    }

    private static void createBd(BaseXClient session) {
        try(BaseXClient.Query query = session.query("for $i in db:get('" +nombreBdXML + "') return $i")) {
            if (query.more()){

            }

        } catch (Exception e) {
            try {
                session.execute("create db " + nombreBdXML);
                System.out.println("Base de datos creada");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }


    }

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
                case 5:
                    mostrarMasCaroPorCategoria(session);
                case 0:
                    System.out.println();
                    break;
                default:
                    System.out.println("La opcion no es correcta");
            }

        }while (opt!=0);
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

    private static void menuConsultasMongo() {
        int opt;

        do{
            opt = pedirInt("""
                    **********************************************************************************************************************************************************************
                    1.- Crear un nuevo cliente (no podrá haber email repetidos). 
                    2.- Identificar cliente según el email. Dado el email se obtendrá el ID del cliente de forma que las siguientes consultas se harán sobre ese cliente.
                    3.- Borrar un cliente.
                    4.- Modificar el valor de un campo de la información del cliente.
                    5.- Añadir producto al carrito del cliente. Se pedirá: id del producto y cantidad, así como si se desea seguir introduciendo más productos.
                    6.- Mostrar el carrito del cliente. Se mostrarán los datos del carrito y el precio total.
                    7.- Mostrar pedidos del cliente.
                    8.- Pagar el carrito de un cliente: se mostrará el carrito junto con una orden de confirmación. Si es positiva los productos pasarán a formar parte de un nuevo pedido.
                    9.- Teniendo en cuenta todos los clientes, calcular el total de la compra para cada carrito y listar los resultados ordenados por el total de forma ascendente.
                    10.-Teniendo en cuenta todos los clientes, obtener el total gastado por cada cliente en todos sus pedidos.
                    0.- SALIR
                    ***********************************************************************************************************************************************************************  
                    """);
            switch (opt){
                case 1:
                    break;
                case 2:
                    break;
                case 0:
                    System.out.println();
                    break;
                default:
                    System.out.println("La opcion no es correcta");
            }

        }while (opt!=0);
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