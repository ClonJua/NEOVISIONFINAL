import { useState, useMemo } from 'react';
import { Calendar, dateFnsLocalizer, Views, type View } from 'react-big-calendar';
import { format, parse, startOfWeek, getDay } from 'date-fns';
import { enUS } from 'date-fns/locale';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import '../styles/calendar.css';
import type { CitaResponse } from '../api/adminService';

const localizer = dateFnsLocalizer({
  format,
  parse,
  startOfWeek,
  getDay,
  locales: { 'en-US': enUS },
});

interface CitaCalendarProps {
  citas: CitaResponse[];
  view?: View;
  onSelectCita?: (cita: CitaResponse) => void;
  onSelectSlot?: (start: Date, end: Date) => void;
}

export default function CitaCalendar({ citas, view = 'month', onSelectCita, onSelectSlot }: CitaCalendarProps) {
  const [currentView, setCurrentView] = useState<View>(view);
  const [currentDate, setCurrentDate] = useState<Date>(new Date());

  const events = useMemo(() => {
    return citas
      .filter(cita => cita.fechaSesion && cita.horaInicio && cita.horaFin)
      .map(cita => {
        const startStr = `${cita.fechaSesion}T${cita.horaInicio}`;
        const endStr = `${cita.fechaSesion}T${cita.horaFin}`;
        return {
          id: cita.idCita,
          title: `${(cita.nombreEstudiante2 ? `${cita.nombreEstudiante} y ${cita.nombreEstudiante2}` : cita.nombreEstudiante) || 'Sin asignar'}${cita.nombrePaciente ? ' - ' + cita.nombrePaciente : ''}`,
          start: new Date(startStr),
          end: new Date(endStr),
          resource: cita,
        };
      });
  }, [citas]);

  const getEventStyleRtn = (event: any) => {
    const cita = event.resource as CitaResponse;
    let backgroundColor = '#10b981';

    if (cita.estado === 'RESERVADO') backgroundColor = '#3b82f6';
    else if (cita.estado === 'COMPLETADO') backgroundColor = '#8b5cf6';
    else if (cita.estado === 'CANCELADO') backgroundColor = '#6b7280';
    else if (cita.estado === 'VENCIDA') backgroundColor = '#ef4444';

    return {
      style: {
        backgroundColor,
        borderRadius: '4px',
        opacity: 0.9,
        color: 'white',
        border: 'none',
        display: 'block',
        fontSize: '12px',
        padding: '2px 4px',
      },
    };
  };

  return (
    <div className="bg-white border border-slate-200 rounded-lg overflow-hidden p-4">
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-semibold text-slate-700">Calendario de Citas</h3>
        <div className="text-sm text-slate-500">
          Total: <span className="font-semibold text-slate-700">{events.length}</span> citas
        </div>
      </div>

      <div className="legend mb-4 flex flex-wrap gap-4 text-xs">
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 bg-green-500 rounded"></div>
          <span>Disponible</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 bg-blue-500 rounded"></div>
          <span>Reservado</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 bg-purple-500 rounded"></div>
          <span>Completado</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 bg-gray-500 rounded"></div>
          <span>Cancelado</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 bg-red-500 rounded"></div>
          <span>Vencida</span>
        </div>
      </div>

      <div style={{ height: '650px' }}>
        <Calendar
          localizer={localizer}
          events={events}
          startAccessor="start"
          endAccessor="end"
          view={currentView}
          onView={(v) => setCurrentView(v)}
          date={currentDate}
          onNavigate={(d) => setCurrentDate(d)}
          views={[Views.MONTH, Views.WEEK, Views.DAY, Views.AGENDA]}
          onSelectEvent={(event: any) => onSelectCita?.(event.resource)}
          onSelectSlot={(slotInfo: any) => onSelectSlot?.(slotInfo.start, slotInfo.end)}
          selectable={!!onSelectSlot}
          popup
          eventPropGetter={getEventStyleRtn}
          style={{ height: '100%' }}
          messages={{
            today: 'Hoy',
            previous: 'Anterior',
            next: 'Siguiente',
            month: 'Mes',
            week: 'Semana',
            day: 'Día',
            agenda: 'Agenda',
            date: 'Fecha',
            time: 'Hora',
            event: 'Evento',
            allDay: 'Todo el día',
            noEventsInRange: 'No hay citas en este rango',
            showMore: (total) => `+${total} más`,
          }}
        />
      </div>
    </div>
  );
}
