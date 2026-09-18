import { useState, useEffect } from 'react';
import { adminService, type CitaResponse } from '../../api/adminService';
import CitaCalendar from '../../components/CitaCalendar';
import { Modal } from '../../components/ui/Modal';

export default function CalendarTab() {
  const [citas, setCitas] = useState<CitaResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedCita, setSelectedCita] = useState<CitaResponse | null>(null);

  useEffect(() => {
    cargar();
  }, []);

  const cargar = async () => {
    try {
      setLoading(true);
      const res = await adminService.listarCitas();
      setCitas(res.data.data);
    } catch (err) {
      console.error('Error cargando citas:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="text-center py-8 text-slate-500">Cargando citas...</div>;
  }

  return (
    <div className="space-y-6">
      <CitaCalendar
        citas={citas}
        view="month"
        onSelectCita={(cita) => setSelectedCita(cita)}
      />

      <Modal open={!!selectedCita} onClose={() => setSelectedCita(null)} title="Detalles de la Cita" size="lg">
        {selectedCita && (
          <>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">ID Cita</label>
                <p className="text-sm text-slate-800 font-mono">{selectedCita.idCita}</p>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Estado</label>
                <p className="text-sm text-slate-800 font-medium">
                  <span className={`inline-block px-2 py-1 rounded text-xs font-medium ${
                    selectedCita.estado === 'DISPONIBLE' ? 'bg-green-100 text-green-700' :
                    selectedCita.estado === 'RESERVADO' ? 'bg-blue-100 text-blue-700' :
                    selectedCita.estado === 'COMPLETADO' ? 'bg-purple-100 text-purple-700' :
                    selectedCita.estado === 'CANCELADO' ? 'bg-gray-100 text-gray-700' :
                    'bg-red-100 text-red-700'
                  }`}>
                    {selectedCita.estado}
                  </span>
                </p>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Fecha</label>
                <p className="text-sm text-slate-800">{selectedCita.fechaSesion}</p>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Horario</label>
                <p className="text-sm text-slate-800">{selectedCita.horaInicio || '-'} - {selectedCita.horaFin || '-'}</p>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Estudiante</label>
                <p className="text-sm text-slate-800">
                  {selectedCita.nombreEstudiante2
                    ? `${selectedCita.nombreEstudiante} y ${selectedCita.nombreEstudiante2}`
                    : selectedCita.nombreEstudiante}
                </p>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Paciente</label>
                <p className="text-sm text-slate-800">{selectedCita.nombrePaciente || '-'}</p>
              </div>
              <div className="col-span-2">
                <label className="block text-xs font-medium text-slate-500 mb-1">Salon</label>
                <p className="text-sm text-slate-800">{selectedCita.nombreSalon || 'Sin asignar'}</p>
              </div>
              <div className="col-span-2">
                <label className="block text-xs font-medium text-slate-500 mb-1">Observaciones</label>
                <p className="text-sm text-slate-800">{selectedCita.observaciones || '-'}</p>
              </div>
            </div>

            {selectedCita.fechaReserva && (
              <div className="mt-4 pt-4 border-t border-slate-200">
                <label className="block text-xs font-medium text-slate-500 mb-1">Fecha de Reserva</label>
                <p className="text-xs text-slate-600">{new Date(selectedCita.fechaReserva).toLocaleString('es-CO')}</p>
              </div>
            )}
          </>
        )}
      </Modal>
    </div>
  );
}
