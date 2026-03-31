-- ============================================
-- Play IME Preset API - Database Setup
-- Run this in your Supabase SQL Editor
-- ============================================

-- 1. Create presets table
CREATE TABLE presets (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  settings JSONB NOT NULL,
  share_code TEXT UNIQUE,
  likes_count INTEGER DEFAULT 0,
  is_default BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

-- 2. Create likes table
CREATE TABLE likes (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  preset_id UUID REFERENCES presets(id) ON DELETE CASCADE NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now(),
  UNIQUE(user_id, preset_id)
);

-- 3. Create indexes
CREATE INDEX idx_presets_user_id ON presets(user_id);
CREATE INDEX idx_presets_share_code ON presets(share_code);
CREATE INDEX idx_presets_is_default ON presets(is_default);
CREATE INDEX idx_presets_likes_count ON presets(likes_count DESC);
CREATE INDEX idx_likes_user_id ON likes(user_id);
CREATE INDEX idx_likes_preset_id ON likes(preset_id);

-- 4. Updated_at trigger
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$;

CREATE TRIGGER presets_updated_at
  BEFORE UPDATE ON presets
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 5. Seed sample data
INSERT INTO presets (name, settings, is_default, share_code) VALUES
('Dark', '{"clock":{"mode":"analog","showSeconds":true,"bgColorOff":"#2c3e50","bgColorOn":"#c0392b","textColor":"#ecf0f1","font":"system-ui","fontSize":24},"indicator":{"colors":{"ja":"#e74c3c","en":"#3498db"},"texts":{"ja":"あ","en":"A"},"size":40}}'::jsonb, TRUE, 'default1'),
('Light', '{"clock":{"mode":"digital","showSeconds":true,"bgColorOff":"#5ac8fa","bgColorOn":"#ff9500","textColor":"#1c1c1e","font":"system-ui","fontSize":28},"indicator":{"colors":{"ja":"#ff3b30","en":"#007aff"},"texts":{"ja":"あ","en":"A"},"size":36}}'::jsonb, TRUE, 'default2');
