from django.urls import path
from . import views

urlpatterns = [
    path('api/ai/v1', views.api_view_example),
    path('api/v1/users/doctors', views.doctors_list),
    path('api/v1/users', views.create_user),
    path('api/v1/users/createDoctor', views.create_doctor, name='create-doctor'),
    path('login', views.login_user),
]