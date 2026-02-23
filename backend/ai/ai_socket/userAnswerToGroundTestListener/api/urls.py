from django.urls import path
from . import views

urlpatterns = [
    path('api/v1/users/createDoctor', views.create_doctor, name='create-doctor'),
    path('api/v1/users', views.create_user, name='create-user'),
    path('api/v1/users/doctors', views.doctors_list, name='doctors-list'),
    path('login', views.login_user, name='login'),
    path('api/v1/ai', views.ai_communication, name='ai-communication'),
]