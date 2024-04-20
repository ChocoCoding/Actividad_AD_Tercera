{
    "_id": ObjectId("cliente_id_1"),
    "nombre": "Nombre Cliente 1",
    "email": "cliente1@example.com",
    "direccion": "Dirección Cliente 1",
    "carrito": [
       {
          "producto_id": 1,
          "nombre": "Laptop HP Pavilion",
          "cantidad": 2,
          "precio_unitario": 799.99
       },
       {
          "producto_id": 3,
          "nombre": "Tablet Lenovo Tab M10",
          "cantidad": 1,
          "precio_unitario": 199.99
       }
    ],
    "pedidos": [
       {
          "pedido_id": ObjectId("pedido_id_1"),
          "productos": [
             {
                "producto_id": 1,
                "nombre": "Laptop HP Pavilion",
                "cantidad": 2,
                "precio_unitario": 799.99
             }
          ],
          "total": 1599.98,
          "fecha_pedido": ISODate("2023-01-01T12:30:00.000Z")
       }
    ]
}
{
    "_id": ObjectId("cliente_id_2"),
    "nombre": "Nombre Cliente 2",
    "email": "cliente2@example.com",
    "direccion": "Dirección Cliente 2",
    "carrito": [
       {
          "producto_id": 2,
          "nombre": "Smartphone Samsung Galaxy S21",
          "cantidad": 1,
          "precio_unitario": 899.99
       }
    ],
    "pedidos": [
       {
          "pedido_id": ObjectId("pedido_id_2"),
          "productos": [
             {
                "producto_id": 2,
                "nombre": "Smartphone Samsung Galaxy S21",
                "cantidad": 1,
                "precio_unitario": 899.99
             }
          ],
          "total": 899.99,
          "fecha_pedido": ISODate("2023-01-02T13:45:00.000Z")
       }
    ]
}
{
    "_id": ObjectId("cliente_id_3"),
    "nombre": "Nombre Cliente 3",
    "email": "cliente3@example.com",
    "direccion": "Dirección Cliente 3",
    "carrito": [
       {
          "producto_id": 4,
          "nombre": "Auriculares Inalámbricos Sony WH-1000XM4",
          "cantidad": 1,
          "precio_unitario": 299.99
       }
    ],
    "pedidos": [
       {
          "pedido_id": ObjectId("pedido_id_3"),
          "productos": [
             {
                "producto_id": 4,
                "nombre": "Auriculares Inalámbricos Sony WH-1000XM4",
                "cantidad": 1,
                "precio_unitario": 299.99
             }
          ],
          "total": 299.99,
          "fecha_pedido": ISODate("2023-01-03T14:00:00.000Z")
       }
    ]
}
{
    "_id": ObjectId("cliente_id_4"),
    "nombre": "Nombre Cliente 4",
    "email": "cliente4@example.com",
    "direccion": "Dirección Cliente 4",
    "carrito": [
       {
          "producto_id": 5,
          "nombre": "Televisor Samsung QLED Q80T",
          "cantidad": 1,
          "precio_unitario": 1299.99
       }
    ],
    "pedidos": [
       {
          "pedido_id": ObjectId("pedido_id_4"),
          "productos": [
             {
                "producto_id": 5,
                "nombre": "Televisor Samsung QLED Q80T",
                "cantidad": 1,
                "precio_unitario": 1299.99
             }
          ],
          "total": 1299.99,
          "fecha_pedido": ISODate("2023-01-04T15:15:00.000Z")
       }
    ]
}