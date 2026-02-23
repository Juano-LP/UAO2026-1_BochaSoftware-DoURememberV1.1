from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status
from django.contrib.auth.models import User
import socket

# Configuración del socket para comunicar con la AI
HOST = '192.168.80.14'  # Ajusta según tu configuración
PORT = 2828

# Crear doctor (is_staff=True)
@api_view(['POST'])
def create_doctor(request):
    data = request.data
    username = data.get('username')
    password = data.get('password')
    email = data.get('email', '')
    first_name = data.get('name', '')
    last_name = data.get('lastname', '')

    if not username or not password:
        return Response({"error": "Username y password son requeridos"}, status=status.HTTP_400_BAD_REQUEST)

    try:
        user = User.objects.create_user(
            username=username,
            password=password,
            email=email,
            first_name=first_name,
            last_name=last_name,
            is_staff=True  # 🔹 Todos los usuarios creados aquí son doctores
        )
        return Response({"status": "doctor created"}, status=status.HTTP_201_CREATED)
    except Exception as e:
        return Response({"error": str(e)}, status=status.HTTP_400_BAD_REQUEST)


# Listar todos los doctores
@api_view(['GET'])
def doctors_list(request):
    doctors = User.objects.filter(is_staff=True)
    data = [{"id": d.id, "username": d.username, "email": d.email,
             "first_name": d.first_name, "last_name": d.last_name} for d in doctors]
    return Response(data)


# Crear usuario normal
@api_view(['POST'])
def create_user(request):
    data = request.data
    username = data.get('username')
    password = data.get('password')
    email = data.get('email', '')
    first_name = data.get('name', '')
    last_name = data.get('lastname', '')

    if not username or not password:
        return Response({"error": "Username y password son requeridos"}, status=status.HTTP_400_BAD_REQUEST)

    try:
        user = User.objects.create_user(
            username=username,
            password=password,
            email=email,
            first_name=first_name,
            last_name=last_name,
            is_staff=False  # Usuario normal
        )
        return Response({"status": "user created"}, status=status.HTTP_201_CREATED)
    except Exception as e:
        return Response({"error": str(e)}, status=status.HTTP_400_BAD_REQUEST)


# Login simple (para frontend)
@api_view(['POST'])
def login_user(request):
    from django.contrib.auth import authenticate
    username = request.data.get('username')
    password = request.data.get('password')

    user = authenticate(username=username, password=password)
    if user:
        return Response({"status": "ok", "username": user.username, "is_staff": user.is_staff})
    return Response({"error": "Credenciales inválidas"}, status=status.HTTP_401_UNAUTHORIZED)


# Ejemplo de comunicación con la AI via socket
@api_view(['POST'])
def ai_communication(request):
    client = None
    try:
        client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        client.connect((HOST, PORT))
        client.send(str(request.data.get('userAnswer')).encode("utf-8"))
        response = client.recv(1024).decode("utf-8")
        return Response({"status": "ok", "response": response})
    except Exception as e:
        return Response({"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
    finally:
        if client:
            try:
                client.shutdown(socket.SHUT_RDWR)
                client.close()
            except Exception:
                pass