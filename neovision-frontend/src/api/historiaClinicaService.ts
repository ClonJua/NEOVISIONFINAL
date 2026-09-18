import api from './axios';
import type { ApiResponse } from './authService';

// ===================== Secciones clínicas (folios 1–4) =====================
// Los nombres de campo coinciden 1:1 con los value objects del backend.

export interface DatosGenerales {
  tipoDocumento?: string; numeroDocumento?: string;
  apellidos?: string; nombres?: string; numeroConsultorio?: string;
  fechaNacimiento?: string; edad?: string; estadoCivil?: string; sexo?: string;
  direccion?: string; localidad?: string; ciudad?: string;
  celular?: string; telefono?: string; ocupacion?: string;
  acompanante?: string; acompananteTelefono?: string;
  responsable?: string; responsableTelefono?: string; responsableParentesco?: string;
  eps?: string; tipoVinculacion?: string;
  remitidoPor?: string; ultimoControlVisual?: string;
}

export interface Anamnesis {
  comoEmpezo?: string; asociadoActividades?: string; antiguedadMolestia?: string;
  ardor?: boolean; cansancio?: boolean; cefalea?: boolean; dolorOcular?: boolean;
  fotofobia?: boolean; lagrimeo?: boolean; visionDoble?: boolean; hiperemia?: boolean;
  secreciones?: boolean; prurito?: boolean; miodesopsias?: boolean;
  confusionAlLeer?: boolean; mareo?: boolean; otros?: boolean; otrosDetalle?: string;
  antecedentesPersonales?: string; antecedentesOculares?: string;
  medicamentos?: string; antecedentesFamiliares?: string;
}

export interface SignosVitales {
  peso?: string; estatura?: string; imc?: string; tensionArterial?: string;
}

export interface Lensometria {
  odEsfera?: string; odCilindro?: string; odEje?: string; odPrisma?: string;
  odBase?: string; odAdicion?: string; odDistMecanica?: string;
  oiEsfera?: string; oiCilindro?: string; oiEje?: string; oiPrisma?: string;
  oiBase?: string; oiAdicion?: string; oiDistMecanica?: string;
  diseno?: string; material?: string; uso?: string; otros?: string;
  distanciaNasoPupilar?: string;
}

export interface AgudezaVisual {
  scOdLejos?: string; scOdCerca?: string; scOdEstenopeico?: string;
  scOiLejos?: string; scOiCerca?: string; scOiEstenopeico?: string;
  scAoLejos?: string; scAoCerca?: string; scAoEstenopeico?: string;
  ccOdLejos?: string; ccOdCerca?: string; ccOdEstenopeico?: string;
  ccOiLejos?: string; ccOiCerca?: string; ccOiEstenopeico?: string;
  ccAoLejos?: string; ccAoCerca?: string; ccAoEstenopeico?: string;
  sistemaNotacion?: string; optotipo?: string;
}

export interface Queratometria {
  ojoDerecho?: string; ojoIzquierdo?: string; mirasOd?: string; mirasOi?: string; av?: string;
  observacionesEInstrumento?: string;
}

export interface Refraccion {
  estaticaOd?: string; estaticaOi?: string; estaticaOdAv?: string; estaticaOiAv?: string; estaticaObservaciones?: string;
  dinamicaOd?: string; dinamicaOi?: string; dinamicaOdAv?: string; dinamicaOiAv?: string; dinamicaObservaciones?: string;
  cicloplegiaOd?: string; cicloplegiaOi?: string; cicloplegiaOdAv?: string; cicloplegiaOiAv?: string; cicloplegiaObservaciones?: string;
  subjetivoOd?: string; subjetivoOi?: string; subjetivoOdAv?: string; subjetivoOiAv?: string; subjetivoObservaciones?: string;
  afinacionOd?: string; afinacionOi?: string; afinacionOdAv?: string; afinacionOiAv?: string; afinacionObservaciones?: string;
  balanceOd?: string; balanceOi?: string; balanceOdAv?: string; balanceOiAv?: string; balanceObservaciones?: string;
  adicionOd?: string; adicionOi?: string; adicionOdAv?: string; adicionOiAv?: string; adicionDistancia?: string; adicionObservaciones?: string;
}

export interface PruebasComplementarias {
  visionCromaticaTest?: string; visionCromaticaOd?: string; visionCromaticaOi?: string;
  estereopsis?: string; estereopsisTest?: string;
  campoVisualTest?: string; campoVisualOd?: string; campoVisualOi?: string;
  sensibilidadContrasteTest?: string; sensibilidadContrasteOd?: string; sensibilidadContrasteOi?: string;
  foriaDistancia?: string; foria40cm?: string; foriaTest?: string; otrasPruebas?: string;
}

export interface FormulaFinal {
  lejosOdEsfera?: string; lejosOdCilindro?: string; lejosOdEje?: string; lejosOdPrisma?: string; lejosOdBase?: string; lejosOdAv?: string;
  lejosOiEsfera?: string; lejosOiCilindro?: string; lejosOiEje?: string; lejosOiPrisma?: string; lejosOiBase?: string; lejosOiAv?: string;
  mediaOdEsfera?: string; mediaOdCilindro?: string; mediaOdEje?: string; mediaOdPrisma?: string; mediaOdBase?: string; mediaOdAv?: string;
  mediaOiEsfera?: string; mediaOiCilindro?: string; mediaOiEje?: string; mediaOiPrisma?: string; mediaOiBase?: string; mediaOiAv?: string;
  cercaOdEsfera?: string; cercaOdCilindro?: string; cercaOdEje?: string; cercaOdPrisma?: string; cercaOdBase?: string; cercaOdAv?: string;
  cercaOiEsfera?: string; cercaOiCilindro?: string; cercaOiEje?: string; cercaOiPrisma?: string; cercaOiBase?: string; cercaOiAv?: string;
  adicionOdEsfera?: string; adicionOdCilindro?: string; adicionOdEje?: string; adicionOdPrisma?: string; adicionOdBase?: string; adicionOdAv?: string;
  adicionOiEsfera?: string; adicionOiCilindro?: string; adicionOiEje?: string; adicionOiPrisma?: string; adicionOiBase?: string; adicionOiAv?: string;
  distanciaNasoPupilar?: string; tipoLenteYMarca?: string; material?: string;
  filtros?: string; color?: string; uso?: string; observaciones?: string;
}

export interface ExamenExterno {
  general?: string;
  orbitaOd?: string; orbitaOi?: string; cejasOd?: string; cejasOi?: string;
  pestanasOd?: string; pestanasOi?: string; parpadosOd?: string; parpadosOi?: string;
  conjuntivaOd?: string; conjuntivaOi?: string; escleraOd?: string; escleraOi?: string;
  corneaOd?: string; corneaOi?: string; irisOd?: string; irisOi?: string;
  pupilaOd?: string; pupilaOi?: string; camaraAnteriorOd?: string; camaraAnteriorOi?: string;
  cristalinoOd?: string; cristalinoOi?: string;
  schirmerOd?: string; schirmerOi?: string; rupturaLagrimalOd?: string; rupturaLagrimalOi?: string;
  tonometriaInstrumento?: string; tonometriaHora?: string; tonometriaOd?: string; tonometriaOi?: string;
  diagramaOd?: string; diagramaOi?: string;
}

export interface Motilidad {
  fotomotorDirectoOd?: string; consensualOd?: string; acomodativoOd?: string;
  fotomotorDirectoOi?: string; consensualOi?: string; acomodativoOi?: string;
  defectoPupilar?: string; ojoDominante?: string;
  kappaOd?: string; kappaOi?: string; hirschberg?: string;
  estadoMotorTest?: string; estadoMotorCorreccion?: string;
  estadoMotor6m?: string; estadoMotor4m?: string; estadoMotor3m?: string; estadoMotor40cm?: string; estadoMotor20cm?: string;
  ppcDefectoReal?: string; ppcLuz?: string; ppcLuzFiltroRojo?: string;
  duccionesOd?: string; duccionesOi?: string;
  diagramaVersionesOd?: string; diagramaVersionesOi?: string;
  observaciones?: string;
}

export interface FondoOjo {
  mediosRefringentesOd?: string; mediosRefringentesOi?: string;
  coloracionFondoOd?: string; coloracionFondoOi?: string;
  papilaExcavacionOd?: string; papilaExcavacionOi?: string;
  relacionArteriaVenaOd?: string; relacionArteriaVenaOi?: string;
  maculaFijacionOd?: string; maculaFijacionOi?: string;
  diagramaOd?: string; diagramaOi?: string;
}

export interface Conducta {
  conducta?: string; remisiones?: string; control?: string;
}

export interface DiagnosticoDto {
  codigoCie10?: string; descripcion?: string; orden?: number;
}

export interface ProcedimientoDto {
  codigoCups?: string; descripcion?: string; orden?: number;
}

// ===================== Requests / Responses =====================

export interface AbrirHistoriaRequest {
  numeroHistoria?: string;
}

export interface GuardarAtencionRequest {
  motivoConsulta?: string;
  datosGenerales?: DatosGenerales;
  anamnesis?: Anamnesis;
  signosVitales?: SignosVitales;
  lensometria?: Lensometria;
  agudezaVisual?: AgudezaVisual;
  queratometria?: Queratometria;
  refraccion?: Refraccion;
  pruebasComplementarias?: PruebasComplementarias;
  formulaFinal?: FormulaFinal;
  examenExterno?: ExamenExterno;
  motilidad?: Motilidad;
  fondoOjo?: FondoOjo;
  conducta?: Conducta;
  diagnosticos?: DiagnosticoDto[];
  procedimientos?: ProcedimientoDto[];
}

export type EstadoAtencion = 'BORRADOR' | 'PENDIENTE_REVISION' | 'DEVUELTA' | 'FINALIZADA';

export interface AdendaResponse {
  idAdenda: number; texto: string; autorCorreo: string; autorRol: string; fecha: string;
}

export interface AtencionResumenResponse {
  idAtencion: number; fechaAtencion: string; estado: EstadoAtencion;
  idPaciente: number | null; nombrePaciente: string | null;
  nombreEstudiante: string | null; nombreDocente: string | null;
  motivoConsulta: string | null; numDiagnosticos: number;
}

export interface HistoriaClinicaResponse {
  idHistoria: number | null;
  idPaciente: number | null;
  nombrePaciente: string | null;
  numeroDocumento: string | null;
  numeroHistoria: string | null;
  fechaApertura: string | null;
  estado: string | null;
  tieneConsentimiento: boolean;
  atenciones: AtencionResumenResponse[];
}

export interface AtencionResponse {
  idAtencion: number; idHistoria: number | null; idPaciente: number | null;
  nombrePaciente: string | null; idCita: number | null; fechaCita: string | null;
  estado: EstadoAtencion; fechaAtencion: string; fechaEnvioRevision: string | null;
  fechaFinalizacion: string | null; motivoDevolucion: string | null;
  idEstudiante: number | null; nombreEstudiante: string | null;
  idDocenteSupervisor: number | null; nombreDocente: string | null;
  motivoConsulta?: string;
  datosGenerales?: DatosGenerales; anamnesis?: Anamnesis; signosVitales?: SignosVitales;
  lensometria?: Lensometria; agudezaVisual?: AgudezaVisual; queratometria?: Queratometria;
  refraccion?: Refraccion; pruebasComplementarias?: PruebasComplementarias;
  formulaFinal?: FormulaFinal; examenExterno?: ExamenExterno; motilidad?: Motilidad;
  fondoOjo?: FondoOjo; conducta?: Conducta;
  diagnosticos: DiagnosticoDto[];
  procedimientos: ProcedimientoDto[];
  firmaEstudiante?: string; firmaDocente?: string; firmaPaciente?: string;
  hashIntegridad?: string;
  adendas: AdendaResponse[];
}

export interface AuditoriaResponse {
  idAuditoria: number; usuarioCorreo: string; rol: string; accion: string;
  entidad: string; entidadId: number | null; fechaHora: string; detalle: string | null;
}

export interface VerificacionIntegridadResponse {
  tieneSello: boolean;
  valido: boolean;
  hashAlmacenado: string | null;
  hashRecalculado: string | null;
  mensaje: string;
}

export interface VerificacionCadenaResponse {
  valida: boolean;
  totalRegistros: number;
  idRegistroAlterado: number | null;
  mensaje: string;
}

export interface AbrirAtencionRequest { idCita: number; }
export interface EnviarRevisionRequest { firmaEstudiante?: string; }
export interface FinalizarAtencionRequest { firmaDocente?: string; firmaPaciente?: string; }
export interface DevolverAtencionRequest { motivo: string; }
export interface AdendaRequest { texto: string; }

export const historiaClinicaService = {
  // Historia
  obtenerPorPaciente: (idPaciente: number) =>
    api.get<ApiResponse<HistoriaClinicaResponse>>(`/historias/paciente/${idPaciente}`),
  abrirHistoria: (idPaciente: number, data: AbrirHistoriaRequest) =>
    api.post<ApiResponse<HistoriaClinicaResponse>>(`/historias/paciente/${idPaciente}/abrir`, data),
  listarTodas: () =>
    api.get<ApiResponse<HistoriaClinicaResponse[]>>('/historias'),
  auditoriaPaciente: (idPaciente: number) =>
    api.get<ApiResponse<AuditoriaResponse[]>>(`/historias/paciente/${idPaciente}/auditoria`),
  verificarCadenaAuditoria: () =>
    api.get<ApiResponse<VerificacionCadenaResponse>>('/historias/auditoria/verificar-cadena'),

  // Atenciones
  abrirAtencion: (data: AbrirAtencionRequest) =>
    api.post<ApiResponse<AtencionResponse>>('/historias/atenciones', data),
  detalleAtencion: (id: number) =>
    api.get<ApiResponse<AtencionResponse>>(`/historias/atenciones/${id}`),
  exportarPdf: (id: number) =>
    api.get<Blob>(`/historias/atenciones/${id}/pdf`, { responseType: 'blob' }),
  verificarIntegridad: (id: number) =>
    api.get<ApiResponse<VerificacionIntegridadResponse>>(`/historias/atenciones/${id}/integridad`),
  guardarBorrador: (id: number, data: GuardarAtencionRequest) =>
    api.put<ApiResponse<AtencionResponse>>(`/historias/atenciones/${id}`, data),
  enviarRevision: (id: number, data: EnviarRevisionRequest) =>
    api.post<ApiResponse<AtencionResponse>>(`/historias/atenciones/${id}/enviar-revision`, data),
  devolver: (id: number, data: DevolverAtencionRequest) =>
    api.post<ApiResponse<AtencionResponse>>(`/historias/atenciones/${id}/devolver`, data),
  finalizar: (id: number, data: FinalizarAtencionRequest) =>
    api.post<ApiResponse<AtencionResponse>>(`/historias/atenciones/${id}/finalizar`, data),
  agregarAdenda: (id: number, data: AdendaRequest) =>
    api.post<ApiResponse<AdendaResponse>>(`/historias/atenciones/${id}/adendas`, data),
  misAtenciones: () =>
    api.get<ApiResponse<AtencionResumenResponse[]>>('/historias/atenciones/mias'),
  bandejaRevision: () =>
    api.get<ApiResponse<AtencionResumenResponse[]>>('/historias/atenciones/bandeja'),
};
