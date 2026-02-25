Como ejecutar el proyecto con su nueva funcionalidad:

Asegurate de tener mysql y dentro de mysql crear una base de datos:
  - db_backend_users
  - comando para crear base de datos: create database db_backend_users;



1. Para levantar el backend necesitas 3 terminales:

  - la primera para ejecutar el sping boot:
      - te vas a la ubicacion: \backend\users-backend>
      - antes de levantar el backend para demostracion de que funciona el sistema de notificaciones:
        - $env:MAIL_USERNAME="tu_correo@gmail.com"
          $env:MAIL_PASSWORD="tu_app_password_de_16_caracteres" (corresponde a la contraseña que debes solicitar luego de haber activado la verificacion de dos pasos en tu cuenta de google, y en el apartado de "contraseña de aplicaciones" crear una contraseña y la contraseña de 16 caracteres sin espacios pegarla ahi)
          $env:NOTIFICATIONS_MAIL_FROM="tu_correo@gmail.com"
        -ejecutas el siguiente comnado:   .\mvnw spring-boot:run
  - la segunda terminal para ejecutar el servidor del backend:
      - navegas a esta ubicacion : \backend\ai>\ai_socket\ :
        - en el ejecutas el siguiente comando (asegurate de tener los archivos requeridos de venv :
        -  si no los tienes descargado ejecuta: python3 -m venv venv
        -   .\venv\Scripts\Activate.ps1 o .\venv\bin\Activate.ps1
        -   posteriormente cuando se active el entorno procedes con el lanzamiento:
           - navegas a esta ruta: \userAnswerToGroundTestListener
           
           - aqui lanza el backend: ..\venv\Scripts\python.exe .\manage.py runserver 0.0.0.0:8000
             
  - la tercera terminal debes navegar : backend\ai\ai_socket>

      - aqui ejecuta python server.py
2. Para levantar el frontend navegas a esto:
    -   frontend\dourememberfront y aqui ejecutas: npm install y posteriormente npm start
3. Para probar el sistema de notificaciones debes tener todo funcionando(punto 1 y 2)
    - Posteriorimente ejeccutar el siguiente comando:
      - Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/notifications/daily-lesson/send-to-email?email=el correo que mandas la notificacion" 
