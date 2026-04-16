import { useTranslation } from 'react-i18next';
import { Card, Tabs } from 'antd';
import { SplitCellsOutlined, MergeCellsOutlined } from '@ant-design/icons';

const { TabPane } = Tabs;

export const SplitMergeTool = () => {
  const { t } = useTranslation();

  return (
    <div className="split-merge-tool-page">
      <Card title={t('Split & Merge Tools')}>
        <Tabs defaultActiveKey="split">
          <TabPane tab={t('Split File')} key="split" icon={<SplitCellsOutlined />}>
            <p>{t('Split large files into smaller chunks')}</p>
          </TabPane>
          <TabPane tab={t('Merge Files')} key="merge" icon={<MergeCellsOutlined />}>
            <p>{t('Merge multiple files into one')}</p>
          </TabPane>
        </Tabs>
      </Card>
    </div>
  );
};
