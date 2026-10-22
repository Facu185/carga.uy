import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthProvider'
import { AvisoSesion } from './componentes/AvisoSesion'
import { RutaProtegida } from './auth/RutaProtegida'
import { Inicio } from './pages/Inicio'
import { NoEncontrado } from './pages/NoEncontrado'
import { LayoutBackoffice } from './pages/backoffice/LayoutBackoffice'
import { Login } from './pages/backoffice/Login'
import { CambiarPassword } from './pages/backoffice/CambiarPassword'
import { PanelBackoffice } from './pages/backoffice/PanelBackoffice'
import { Usuarios } from './pages/backoffice/Usuarios'
import { Funcionarios } from './pages/backoffice/Funcionarios'
import { Nodos } from './pages/backoffice/Nodos'
import { Auditoria } from './pages/backoffice/Auditoria'
import { Ingresar } from './pages/frontoffice/Ingresar'
import { IngresoCallback } from './pages/frontoffice/IngresoCallback'
import { MiCuenta } from './pages/frontoffice/MiCuenta'
import { NuevaGuia } from './pages/frontoffice/NuevaGuia'
import { MisGuias } from './pages/frontoffice/MisGuias'
import { Empresa } from './pages/frontoffice/Empresa'
import { PerfilEmpresa } from './pages/frontoffice/PerfilEmpresa'
import { VehiculosEmpresa } from './pages/frontoffice/VehiculosEmpresa'
import { GuiasEmpresa } from './pages/frontoffice/GuiasEmpresa'
import { SolicitudesEmpresa } from './pages/backoffice/SolicitudesEmpresa'
import { ROLES } from './tipos'

export default function App() {
  return (
    <AuthProvider>
      <AvisoSesion />
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Inicio />} />
          <Route path="/ingresar" element={<Ingresar />} />
          <Route path="/ingreso" element={<IngresoCallback />} />
          <Route
            path="/mi-cuenta"
            element={
              <RutaProtegida roles={[ROLES.CIUDADANO, ROLES.CHOFER, ROLES.FUNCIONARIO]} login="/ingresar">
                <MiCuenta />
              </RutaProtegida>
            }
          />
          <Route
            path="/guias/nueva"
            element={
              <RutaProtegida roles={[ROLES.CIUDADANO]} login="/ingresar">
                <NuevaGuia />
              </RutaProtegida>
            }
          />
          <Route
            path="/mis-guias"
            element={
              <RutaProtegida roles={[ROLES.CHOFER]} login="/ingresar">
                <MisGuias />
              </RutaProtegida>
            }
          />
          <Route
            path="/mi-empresa"
            element={
              <RutaProtegida roles={[ROLES.CIUDADANO, ROLES.CHOFER]} login="/ingresar">
                <Empresa />
              </RutaProtegida>
            }
          />
          <Route
            path="/mi-empresa/perfil"
            element={
              <RutaProtegida roles={[ROLES.CIUDADANO, ROLES.CHOFER]} login="/ingresar">
                <PerfilEmpresa />
              </RutaProtegida>
            }
          />
          <Route
            path="/mi-empresa/vehiculos"
            element={
              <RutaProtegida roles={[ROLES.CIUDADANO, ROLES.CHOFER]} login="/ingresar">
                <VehiculosEmpresa />
              </RutaProtegida>
            }
          />
          <Route
            path="/mi-empresa/guias"
            element={
              <RutaProtegida roles={[ROLES.CIUDADANO, ROLES.CHOFER]} login="/ingresar">
                <GuiasEmpresa />
              </RutaProtegida>
            }
          />
          <Route path="/backoffice/login" element={<Login />} />
          <Route
            path="/backoffice/cambiar-password"
            element={
              <RutaProtegida roles={[ROLES.ADMINISTRADOR, ROLES.AUTORIDAD, ROLES.FUNCIONARIO]}>
                <CambiarPassword />
              </RutaProtegida>
            }
          />
          <Route
            path="/backoffice"
            element={
              <RutaProtegida roles={[ROLES.ADMINISTRADOR, ROLES.AUTORIDAD, ROLES.FUNCIONARIO]}>
                <LayoutBackoffice />
              </RutaProtegida>
            }
          >
            <Route index element={<PanelBackoffice />} />
            <Route
              path="usuarios"
              element={
                <RutaProtegida roles={[ROLES.ADMINISTRADOR]}>
                  <Usuarios />
                </RutaProtegida>
              }
            />
            <Route path="auditoria" element={<Auditoria />} />
            <Route
              path="solicitudes-empresa"
              element={
                <RutaProtegida roles={[ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO]}>
                  <SolicitudesEmpresa />
                </RutaProtegida>
              }
            />
            <Route
              path="funcionarios"
              element={
                <RutaProtegida roles={[ROLES.ADMINISTRADOR]}>
                  <Funcionarios />
                </RutaProtegida>
              }
            />
            <Route
              path="nodos"
              element={
                <RutaProtegida roles={[ROLES.ADMINISTRADOR]}>
                  <Nodos />
                </RutaProtegida>
              }
            />
          </Route>
          <Route path="*" element={<NoEncontrado />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
