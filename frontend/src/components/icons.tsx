// Icons copied path-for-path from the design's inline SVGs.
import Svg, { Circle, Path, Rect } from 'react-native-svg';
import { colors } from '../theme';

type IconProps = { size?: number; color?: string; strokeWidth?: number };

const base = (size: number) => ({
  width: size,
  height: size,
  viewBox: '0 0 24 24',
  fill: 'none',
});

export function SearchIcon({ size = 19, color = colors.muted, strokeWidth = 2 }: IconProps) {
  return (
    <Svg {...base(size)} stroke={color} strokeWidth={strokeWidth} strokeLinecap="round">
      <Circle cx={11} cy={11} r={6} />
      <Path d="M15.5 15.5L20 20" />
    </Svg>
  );
}

export function BackIcon({ size = 24, color = colors.text, strokeWidth = 2 }: IconProps) {
  return (
    <Svg {...base(size)} stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round">
      <Path d="M19 12H5M11 6l-6 6 6 6" />
    </Svg>
  );
}

export function PinIcon({ size = 19, color = colors.primary, strokeWidth = 1.8 }: IconProps) {
  return (
    <Svg {...base(size)} stroke={color} strokeWidth={strokeWidth}>
      <Path d="M12 21s-6-5.3-6-10a6 6 0 1112 0c0 4.7-6 10-6 10z" />
      <Circle cx={12} cy={11} r={2} />
    </Svg>
  );
}

export function ShieldCheckIcon({ size = 22, color = '#FFFFFF', strokeWidth = 1.8 }: IconProps) {
  return (
    <Svg {...base(size)} stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round">
      <Path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z" />
      <Path d="M9 12l2 2 4-4" />
    </Svg>
  );
}

export function CctvIcon({ size = 21, color = colors.primary, strokeWidth = 1.8 }: IconProps) {
  return (
    <Svg {...base(size)} stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round">
      <Rect x={3} y={7} width={12} height={10} rx={2.5} />
      <Path d="M15 10.5l6-3v9l-6-3" />
    </Svg>
  );
}

export function LampIcon({ size = 21, color = colors.amberText, strokeWidth = 1.8 }: IconProps) {
  return (
    <Svg {...base(size)} stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round">
      <Path d="M7 9a5 5 0 0110 0H7z" />
      <Path d="M12 9v9" />
      <Path d="M8 21h8" />
    </Svg>
  );
}

export function PersonPinIcon({ size = 21, color = '#7C3AED', strokeWidth = 1.8 }: IconProps) {
  return (
    <Svg {...base(size)} stroke={color} strokeWidth={strokeWidth} strokeLinecap="round">
      <Circle cx={12} cy={8} r={4.5} />
      <Path d="M12 12.5V21M9 17.5h6" />
    </Svg>
  );
}

export function MenuIcon({ size = 18, color = colors.text2, strokeWidth = 2 }: IconProps) {
  return (
    <Svg {...base(size)} stroke={color} strokeWidth={strokeWidth} strokeLinecap="round">
      <Path d="M4 7h16M4 12h16M4 17h16" />
    </Svg>
  );
}

export function PlusIcon({ size = 16, color = '#FFFFFF', strokeWidth = 2.4 }: IconProps) {
  return (
    <Svg {...base(size)} stroke={color} strokeWidth={strokeWidth} strokeLinecap="round">
      <Path d="M12 5v14M5 12h14" />
    </Svg>
  );
}

export function CloseIcon({ size = 16, color = colors.text2, strokeWidth = 2 }: IconProps) {
  return (
    <Svg {...base(size)} stroke={color} strokeWidth={strokeWidth} strokeLinecap="round">
      <Path d="M6 6l12 12M18 6L6 18" />
    </Svg>
  );
}

// Tab bar
export function HomeIcon({ size = 23, color, strokeWidth = 1.8 }: IconProps) {
  return (
    <Svg {...base(size)} stroke={color} strokeWidth={strokeWidth}>
      <Path d="M4 11l8-7 8 7v9h-6v-6h-4v6H4z" />
    </Svg>
  );
}

export function MapIcon({ size = 23, color, strokeWidth = 1.8 }: IconProps) {
  return (
    <Svg {...base(size)} stroke={color} strokeWidth={strokeWidth}>
      <Path d="M9 4L3 6v14l6-2 6 2 6-2V4l-6 2-6-2z" />
      <Path d="M9 4v14M15 6v14" />
    </Svg>
  );
}

export function BellIcon({ size = 23, color, strokeWidth = 1.8 }: IconProps) {
  return (
    <Svg {...base(size)} stroke={color} strokeWidth={strokeWidth}>
      <Path d="M6 16v-5a6 6 0 0112 0v5l2 3H4l2-3z" />
      <Path d="M10 21h4" />
    </Svg>
  );
}

export function UserIcon({ size = 23, color, strokeWidth = 1.8 }: IconProps) {
  return (
    <Svg {...base(size)} stroke={color} strokeWidth={strokeWidth}>
      <Circle cx={12} cy={8} r={4} />
      <Path d="M4 21c1.5-4 5-5 8-5s6.5 1 8 5" />
    </Svg>
  );
}
