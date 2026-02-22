# api/views.py

from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status
import socket
from django.contrib.auth.models import User
from django.contrib.auth import authenticate
from django.http import JsonResponse

# -------------------------------
# Configuración del socket AI
# -------------------------------
HOST = '192.168.80.14'  # IP donde corre tu backend AI
PORT = 2828             # Puerto del backend AI

# -------------------------------
# Función para comunicar con AI
# -------------------------------
@api_view(['POST'])
def api_view_example(request):
    client = None
    try:
        client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        client.connect((HOST, PORT))
        print("Datos recibidos del frontend:", request.data)
        client.send(str(request.data.get('userAnswer')).encode("utf-8"))
        response = client.recv(1024).decode("utf-8")
        print("Respuesta del AI:", response)
        return Response({"status": "ok", "response": response})
    
    except Exception as e:
        return Response(
            {"error": str(e)},
            status=status.HTTP_500_INTERNAL_SERVER_ERROR
        )
    finally:
        if client:
            try:
                client.shutdown(socket.SHUT_RDWR)
                client.close()
            except Exception:
                pass

# -------------------------------
# Lista de doctores
# -------------------------------
@api_view(['GET'])
def doctors_list(request):
    data = [
        {"id": 1, "name": "Dr. Smith"},
        {"id": 2, "name": "Dr. Jones"},
        {"id": 3, "name": "Dr. Alicia"},
    ]
    return JsonResponse(data, safe=False)

# -------------------------------
# Crear usuario
# -------------------------------
@api_view(['POST'])
def create_user(request):
    data = request.data
    username = data.get("username")
    password = data.get("password")
    if not username or not password:
        return JsonResponse({"status": "missing_fields"}, status=400)
    if User.objects.filter(username=username).exists():
        return JsonResponse({"status": "exists"}, status=400)
    
    User.objects.create_user(username=username, password=password)
    return JsonResponse({"status": "ok"})

# -------------------------------
# Login de usuario
# -------------------------------
@api_view(['POST'])
def login_user(request):
    data = request.data
    username = data.get("username")
    password = data.get("password")
    if not username or not password:
        return JsonResponse({"status": "missing_fields"}, status=400)
    
    user = authenticate(username=username, password=password)
    if user:
        return JsonResponse({"status": "ok"})
    else:
        return JsonResponse({"status": "unauthorized"}, status=401)
    
@api_view(['POST'])
def create_doctor(request):
    data = request.data
    username = data.get('username')
    password = data.get('password')
    email = data.get('email')
    first_name = data.get('name')
    last_name = data.get('lastname')

    if not username or not password:
        return Response({"error": "Username and password required"}, status=status.HTTP_400_BAD_REQUEST)

    if User.objects.filter(username=username).exists():
        return Response({"error": "User already exists"}, status=status.HTTP_400_BAD_REQUEST)

    user = User.objects.create_user(username=username, password=password, email=email,
                                    first_name=first_name, last_name=last_name)
    # Aquí puedes agregar perfil o grupo "doctor" si lo tienes implementado
    return Response({"message": "Doctor created", "user_id": user.id}, status=status.HTTP_201_CREATED)